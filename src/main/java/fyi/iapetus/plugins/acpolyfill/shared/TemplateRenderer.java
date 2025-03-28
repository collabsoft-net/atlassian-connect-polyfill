package fyi.iapetus.plugins.acpolyfill.shared;

import com.atlassian.plugin.Plugin;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.user.UserKey;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.velocity.htmlsafe.HtmlFragment;
import com.atlassian.webresource.api.UrlMode;
import com.atlassian.webresource.api.assembler.PageBuilderService;
import com.atlassian.webresource.api.assembler.WebResourceAssemblerFactory;
import com.atlassian.webresource.api.assembler.WebResourceAssembler;
import com.atlassian.webresource.api.assembler.WebResourceSet;
import com.atlassian.webresource.api.assembler.resource.ResourcePhase;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fyi.iapetus.plugins.acpolyfill.UserThemeService;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressFBWarnings("EI_EXPOSE_REP2")
public class TemplateRenderer {

    private final LicenseHelper licenseHelper;
    private final PluginHelper pluginHelper;
    private final UrlHelper urlHelper;
    private final PlatformHelper platformHelper;
    private final UserManager userManager;
    private final PageBuilderService pageBuilderService;
    private final WebResourceAssemblerFactory webResourceAssemblerFactory;
    private final UserThemeService userThemeService;

    public enum RenderType { NORMAL, EMBEDDED }

    public TemplateRenderer(
            LicenseHelper licenseHelper,
            PluginHelper pluginHelper,
            ApplicationProperties applicationProperties,
            UserManager userManager,
            PageBuilderService pageBuilderService,
            WebResourceAssemblerFactory webResourceAssemblerFactory,
            UserThemeService userThemeService
    ) {
        this.licenseHelper = licenseHelper;
        this.pluginHelper = pluginHelper;
        this.urlHelper = new UrlHelper(licenseHelper, applicationProperties);
        this.platformHelper = new PlatformHelper(applicationProperties);
        this.userManager = userManager;
        this.pageBuilderService = pageBuilderService;
        this.webResourceAssemblerFactory = webResourceAssemblerFactory;
        this.userThemeService = userThemeService;
    }

    public String renderAtlassianConnectHost(HttpServletRequest req) throws IOException, URISyntaxException {
        return this.renderAtlassianConnectHost(req, RenderType.NORMAL);
    }

    public String renderAtlassianConnectHost(HttpServletRequest req, RenderType type) throws IOException, URISyntaxException {
        String moduleKey = urlHelper.getModuleKey(req);
        return this.renderAtlassianConnectHost(req, moduleKey, new String[]{}, type);
    }

    public String renderAtlassianConnectHost(HttpServletRequest req, String moduleKey, String[] parameters) throws IOException, URISyntaxException {
        return this.renderAtlassianConnectHost(req, moduleKey, parameters, RenderType.NORMAL);
    }

    public String renderAtlassianConnectHost(HttpServletRequest req, String moduleKey, String[] parameters, RenderType type) throws IOException, URISyntaxException {
        // Get the plugin from the OSGi bridge
        Plugin plugin = pluginHelper.getPlugin();

        // If we did not get the plugin, we are in Servlet mode and should retrieve it from the HttpServletRequest
        if (null == plugin) {
            // Get the app key from the path
            String appKey = urlHelper.getAppKey(req);

            // Get the plugin from the request object
            plugin = pluginHelper.getPlugin(appKey);

            // No plugin, no glory
            if (null == plugin) {
                throw new IOException(String.format("Failed to retrieve plugin by key '%s'", appKey));
            }
        }

        // Get descriptor metadata
        Map<String, String> params = pluginHelper.getModuleParams(plugin, moduleKey);
        params.put("base-url", urlHelper.getBaseUrl());
        params.put("context-path", urlHelper.getContextPath());
        params.put("product-name", platformHelper.getPlatformName());
        params.put("atl-product-name", platformHelper.getPlatformName());
        params.put(String.format("%s-lic", plugin.getKey()), licenseHelper.getLicenseState(plugin));
        params.putAll(urlHelper.getACQueryStringParameters(req));

        if (null != userManager.getRemoteUserKey()) {
            UserKey remoteUserKey = userManager.getRemoteUserKey();
            String colorMode = userThemeService.getColorMode(remoteUserKey);
            Optional<String> userKey = Optional.of(remoteUserKey.getStringValue());

            params.put("remote-user-key", userKey.orElse(""));
            params.put("ajs-remote-user-key", userKey.orElse(""));
            params.put("data-color-mode", colorMode);
        }

        String[] defaultQueryStringParameters = urlHelper.getDefaultQueryStringParameters(req, plugin);
        String[] queryStringParameters = Arrays.copyOf(defaultQueryStringParameters, defaultQueryStringParameters.length + parameters.length);
        System.arraycopy(parameters, 0, queryStringParameters, defaultQueryStringParameters.length, parameters.length);

        URI baseUrl = getBaseUrl(plugin);
        URI moduleUrl = new URI(params.getOrDefault("url", "/"));

        if (null == baseUrl) {
            throw new IOException("Parameter 'ac.baseurl' is required. Please make sure to add this to the `plugin-info` section of the atlassian-plugin.xml");
        }

        // Create a context map using module parameters and query string parameters
        Map<String, String> context = new HashMap<>(params);
        Arrays.stream(queryStringParameters).forEach(item -> {
            String[] parts = item.split("=");
            if (parts.length > 1) {
                context.put(parts[0], parts[1]);
            }
        });

        // Now try to replace all variables ($name) in the parameters based on the available context
        params.replaceAll((key, value) -> replaceVariables(value, context));

        // Generate the url prefix and suffix, based on the context path and the query string parameters
        String urlPrefix = moduleUrl.isAbsolute()
                ? moduleUrl.toString()
                : baseUrl.isAbsolute()
                    ? baseUrl.toString()
                    : null != req.getContextPath()
                        ? Paths.get(req.getContextPath(), baseUrl.toString()).toString()
                        : baseUrl.toString();

        String includeQueryString = params.getOrDefault("ac.include.querystring", "false");
        String urlSuffix = !moduleUrl.isAbsolute()
            ? String.format("%s%s%s", moduleUrl, moduleUrl.toString().contains("?") ? "&" : "?", String.join("&", queryStringParameters))
            : includeQueryString.equalsIgnoreCase("true")
                ? String.format("%s%s", moduleUrl.toString().contains("?") ? "&" : "?", String.join("&", queryStringParameters))
                : "";

        // Start constructing the template
        StringBuilder sb = new StringBuilder();

        // Confluence does not like the HTML padding
        if (platformHelper.isConfluence()) {
            sb.append(this.getMetaTags(params));
            sb.append(this.getResourceTags(type, plugin));
            sb.append(this.getFrameTag(plugin.getKey(), moduleKey, urlPrefix, urlSuffix));
        } else {
            sb.append("<html>");
            sb.append("<head>");
            sb.append(this.getMetaTags(params));
            sb.append(this.getResourceTags(type, plugin));
            sb.append("</head>");
            sb.append("<body>");
            sb.append(this.getFrameTag(plugin.getKey(), moduleKey, urlPrefix, urlSuffix));
            sb.append("</body>");
            sb.append("</html>");
        }

        return sb.toString();
    }

    private HtmlFragment getMetaTags(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        params.forEach((k, v) -> sb.append(String.format("<meta name=\"%s\" content=\"%s\">", k, v)));
        return new HtmlFragment(sb.toString());
    }

    private HtmlFragment getResourceTags(RenderType type, Plugin plugin) throws IOException {
        // Get the resourceContext for the plugin
        String resourceContext = pluginHelper.getContext(plugin);

        // Check if we have a resourceContext. If so, either inject it through the pageBuilderService, or into the HTML
        if (null != resourceContext) {

            // Sometimes the iframe is injected into the page to emulate Atlassian Connect
            // This embedded view does not have access to the PageBuilderService as the page was already built
            // We should inject the resourceContext into the HTML instead
            if (type == RenderType.EMBEDDED) {
                return this.getResources(resourceContext);
            } else {
                pageBuilderService.assembler().resources().requireContext(ResourcePhase.DEFER, resourceContext);
            }
        }

        return new HtmlFragment("");
    }

    private String getFrameTag(String appKey, String moduleKey, String urlPrefix, String urlSuffix) {
        return String.format("<iframe src=\"%1$s%2$s\" style=\"border:none; overflow: hidden; width: 100%%;\" data-ap-appkey=\"%3$s\" data-ap-key=\"%4$s\"></iframe>", urlPrefix, urlSuffix, appKey, moduleKey);
    }

    private HtmlFragment getResources(String identifier) throws IOException {
        Writer writer = new StringWriter();
        WebResourceAssembler assembler = webResourceAssemblerFactory.create().includeSyncbatchResources(false).includeSuperbatchResources(false).autoIncludeFrontendRuntime(false).build();
        assembler.resources().requireContext(ResourcePhase.DEFER, identifier);
        WebResourceSet resources = assembler.assembled().drainIncludedResources();
        resources.writeHtmlTags(writer, UrlMode.AUTO);

        writer.flush();
        return new HtmlFragment(writer.toString());
    }

    private URI getBaseUrl(Plugin plugin) {
        try {
            String baseUrl = pluginHelper.getBaseUrl(plugin);
            return new URI(baseUrl);
        } catch (URISyntaxException ignored) {
            return null;
        }
    }

    private String replaceVariables(String value, Map<String, String> replacements) {
        Pattern regex = Pattern.compile("\\$(.*?)($|\\s)");
        Matcher regexMatcher = regex.matcher(value);
        while(regexMatcher.find()) {
            String matchedKey = regexMatcher.group(1);
            if (replacements.containsKey(matchedKey)) {
                value = value.replace("$" + matchedKey, replacements.get(matchedKey));
            }
        }
        return value;
    }


}
