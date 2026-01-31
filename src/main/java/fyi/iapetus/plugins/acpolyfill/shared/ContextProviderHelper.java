package fyi.iapetus.plugins.acpolyfill.shared;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.osgi.bridge.external.PluginRetrievalService;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.web.context.HttpContext;
import com.atlassian.upm.api.license.PluginLicenseManager;
import com.atlassian.webresource.api.assembler.PageBuilderService;
import com.atlassian.webresource.api.assembler.resource.ResourcePhase;
import fyi.iapetus.plugins.acpolyfill.UserThemeService;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static fyi.iapetus.plugins.acpolyfill.shared.OsgiServices.importOsgiService;

public class ContextProviderHelper {

    private HttpServletRequest httpServletRequest;
    private final UrlHelper urlHelper;
    private final PluginHelper pluginHelper;
    private final PlatformHelper platformHelper;
    private final ApplicationProperties applicationProperties;
    private final PageBuilderService pageBuilderService;
    private final UserManager userManager;
    private final UserThemeService userThemeHelper;


    public ContextProviderHelper() {
        // Optional imports
        PluginAccessor pluginAccessor = OsgiServices.importOsgiService(PluginAccessor.class);
        PluginRetrievalService pluginRetrievalService = OsgiServices.importOsgiService(PluginRetrievalService.class);

        // Required imports
        PluginLicenseManager pluginLicenseManager = Objects.requireNonNull(OsgiServices.importOsgiService(PluginLicenseManager.class));
        this.applicationProperties = Objects.requireNonNull(OsgiServices.importOsgiService(ApplicationProperties.class));
        this.pageBuilderService = Objects.requireNonNull(OsgiServices.importOsgiService(PageBuilderService.class));
        this.userManager = Objects.requireNonNull(OsgiServices.importOsgiService(UserManager.class));
        this.userThemeHelper = Objects.requireNonNull(OsgiServices.importOsgiService(UserThemeService.class));

        // Derived implementations
        LicenseHelper licenseHelper = new LicenseHelper(pluginLicenseManager);
        this.urlHelper = new UrlHelper(licenseHelper, applicationProperties);
        this.pluginHelper = new PluginHelper(pluginRetrievalService, pluginAccessor, urlHelper);
        this.platformHelper = new PlatformHelper(applicationProperties);

        // Check if we can get the HttpServletRequest from the HttpContext
        HttpContext httpContext = importOsgiService(HttpContext.class);
        if (null != httpContext) {
            this.httpServletRequest = httpContext.getRequest();
        }
    }

    public ContextProviderHelper(HttpServletRequest httpServletRequest) {
        this();
        if (null != httpServletRequest) {
            this.httpServletRequest = httpServletRequest;
        }
    }

    public Map getContextMap(Map<String, Object> params) throws IOException {
        // Get the plugin from the OSGi bridge
        Plugin plugin = pluginHelper.getPlugin();

        // It should not be possible that the plugin is unavailable through the OSGi bridge
        // However, if this is the case, check if we can find the plugin through the app key
        if (null == plugin) {
            // Check if the app key is part of the context provider parameters
            String appKey = (String) params.getOrDefault("ac.plugin.key", null);
            if (null == appKey && null != httpServletRequest) {
                // Get the app key from the path
                appKey = urlHelper.getAppKey(httpServletRequest);
            }

            // Make sure that the app key is available
            if (null == appKey) {
                throw new IllegalArgumentException("Failed to determine plugin key. Please provide 'ac.plugin.key' as parameter of the context provider in atlassian-plugin.xml");
            }

            // Now that we have an app key, try to get the plugin again
            plugin = pluginHelper.getPlugin(appKey);

            // No plugin, no glory
            if (null == plugin) {
                throw new IOException(String.format("Failed to retrieve plugin by key '%s'", appKey));
            }
        }

        // Check if the module key is part of the context provider parameters
        // This is required for Issue panel modules
        String moduleKey = (String) params.getOrDefault("ac.moduleKey", null);
        if (null == moduleKey && null != httpServletRequest) {
            moduleKey = urlHelper.getModuleKey(httpServletRequest);
        }

        // Make sure that the module key is available
        if (null == moduleKey) {
            throw new IllegalArgumentException("Failed to determine module. Please provide 'ac.moduleKey' as parameter of the context provider in atlassian-plugin.xml");
        }

        params.put("ac.plugin.key", plugin.getKey());
        params.put("ac.plugin.version", plugin.getPluginInformation().getVersion());
        params.put("ac.moduleKey", moduleKey);

        if (null != httpServletRequest) {
            params.put("ac.queryString", String.join("&", urlHelper.getDefaultQueryStringParameters(httpServletRequest, plugin)));
        }

        // Get the base URL from the plugin-info section of the atlassian-plugin.xml
        // Only set the parameter if the baseurl is set to allow the application to throw an error if it is missing
        String baseUrl = pluginHelper.getBaseUrl(plugin);
        if (null != baseUrl) {
            params.put("ac.baseurl", baseUrl);
        }

        // Get the context from the plugin-info section of the atlassian-plugin.xml
        String resourceContext = pluginHelper.getContext(plugin);
        if (null != resourceContext) {
            params.put("ac.context", resourceContext);
        }

        // Add the HTML that can be used in the following Velocity template:
        // <resource name="view" type="velocity"><![CDATA[$ACHtml]]></resource>
        // Map key must end with 'Html' to prevent velocity from HTML-escaping the output.
        params.put("ACHtml", getHtml(params));

        return new HashMap<>(params);
    }

    public String getHtml(Map<String, Object> context) {
        try {
            StringBuilder sb = new StringBuilder();

            // Only preserve the parameters that are strings
            Map<String, String> params = context
                    .entrySet()
                    .stream()
                    .filter(entry -> entry.getValue() instanceof String)
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> String.valueOf(e.getValue())));

            // Get the plugin key
            String appKey = params.getOrDefault("ac.plugin.key", null);
            if (null == appKey) throw new IOException("500 Server Error - Failed to determine the plugin key");

            // Get the plugin version
            String appVersion = params.getOrDefault("ac.plugin.version", null);
            if (null == appVersion) throw new IOException("500 Server Error - Failed to determine the plugin version");

            // Get module key
            String moduleKey = params.getOrDefault("ac.moduleKey", null);
            if (null == moduleKey) throw new IOException("500 Server Error - Failed to determine the module key");

            // Get the default query string parameters for Atlassian Connect
            String defaultQueryStringParameters = params.getOrDefault("ac.queryString", "");

            // Get Atlassian Connect polyfill using the PageBuilderService
            // Otherwise, existing Frontend APIs are overwritten and all hell breaks loose
            String resourceContext = params.getOrDefault("ac.context", null);
            if (null != resourceContext) {
                pageBuilderService.assembler().resources().requireContext(ResourcePhase.DEFER, resourceContext);
            }

            // Add some additional META information
            Optional<String> userKey = Optional.of(Objects.requireNonNull(userManager.getRemoteUserKey()).getStringValue());
            params.put("remote-user-key", userKey.orElse(""));
            params.put("ajs-remote-user-key", userKey.orElse(""));
            params.put("product-name", platformHelper.getPlatformName());
            params.put("atl-product-name", platformHelper.getPlatformName());
            if (null != userManager.getRemoteUserKey()) {
                String colorMode = userThemeHelper.getColorMode(userManager.getRemoteUserKey());
                params.put("data-color-mode", colorMode);
            }

            String moduleUrl = params.getOrDefault("url", "/");
            String baseUrl = params.getOrDefault("ac.baseurl", null);
            String contextPath = this.applicationProperties.getBaseUrl(com.atlassian.sal.api.UrlMode.RELATIVE_CANONICAL);

            if (null == baseUrl) {
                throw new IOException("500 Server Error - Required parameter 'ac.baseurl' is required. Please make sure to add this to the `plugin-info` section of the atlassian-plugin.xml");
            }

            String urlPrefix = !contextPath.isEmpty() ? Paths.get(contextPath, baseUrl).toString() : baseUrl;
            String urlSuffix = String.format("%s%s%s", moduleUrl, moduleUrl.contains("?") ? "&" : "?", String.join("&", defaultQueryStringParameters));

            // Web panels will break if you pad them with HTML/HEAD/BODY tags
            // Filter out any "private" parameters (prefixed with AC)
            params.forEach((key, value) -> {
                if (!key.toLowerCase().startsWith("ac")) {
                    sb.append(String.format("<meta name=\"%s\" content=\"%s\">", key, value));
                }
            });

            sb.append(String.format("<iframe src=\"%1$s%2$s\" style=\"border:none; overflow: hidden; width: 100%%;\" data-ap-appkey=\"%3$s\" data-ap-key=\"%4$s\"></iframe>", urlPrefix, urlSuffix, appKey, moduleKey));
            return sb.toString();
        } catch (IOException err) {
            return String.format("<html><body>%s</body></html>", err.getMessage());
        }
    }

}
