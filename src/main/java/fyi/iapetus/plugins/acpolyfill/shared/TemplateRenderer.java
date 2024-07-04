package fyi.iapetus.plugins.acpolyfill.shared;

import com.atlassian.plugin.Plugin;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.webresource.api.assembler.PageBuilderService;
import com.atlassian.webresource.api.assembler.resource.ResourcePhase;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

@SuppressFBWarnings("EI_EXPOSE_REP2")
public class TemplateRenderer {

    private final PluginHelper pluginHelper;
    private final UrlHelper urlHelper;
    private final PlatformHelper platformHelper;
    private final UserManager userManager;
    private final PageBuilderService pageBuilderService;

    public TemplateRenderer(
            PluginHelper pluginHelper,
            ApplicationProperties applicationProperties,
            UserManager userManager,
            PageBuilderService pageBuilderService
    ) {
        this.pluginHelper = pluginHelper;
        this.urlHelper = new UrlHelper(pluginHelper, applicationProperties);
        this.platformHelper = new PlatformHelper(applicationProperties);
        this.userManager = userManager;
        this.pageBuilderService = pageBuilderService;
    }

    public String renderAtlassianConnectHost(HttpServletRequest req) throws IOException {
        String moduleKey = urlHelper.getModuleKey(req);
        return this.renderAtlassianConnectHost(req, moduleKey, new String[]{});
    }

    public String renderAtlassianConnectHost(HttpServletRequest req, String moduleKey, String[] parameters) throws IOException {
        StringBuilder sb = new StringBuilder();

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

        // If provided, add the AC context to the page builder service assembler
        String resourceContext = pluginHelper.getContext(plugin);
        if (null != resourceContext) {
            pageBuilderService.assembler().resources().requireContext(ResourcePhase.DEFER, resourceContext);
        }

        // Get descriptor metadata
        Map<String, String> params = pluginHelper.getModuleParams(plugin, moduleKey);
        Optional<String> userKey = Optional.of(Objects.requireNonNull(userManager.getRemoteUserKey()).getStringValue());
        params.put("ajs-remote-user-key", userKey.orElse(""));
        params.put("atl-product-name", platformHelper.getPlatformName());
        params.put(String.format("%s-lic", plugin.getKey()), pluginHelper.getLicenseState(plugin));

        String[] defaultQueryStringParameters = urlHelper.getDefaultQueryStringParameters(req, plugin);
        String[] queryStringParameters = Arrays.copyOf(defaultQueryStringParameters, defaultQueryStringParameters.length + parameters.length);
        System.arraycopy(parameters, 0, queryStringParameters, defaultQueryStringParameters.length, parameters.length);

        String baseUrl = pluginHelper.getBaseUrl(plugin);
        String moduleUrl = params.getOrDefault("url", "/");

        if (null == baseUrl) {
            throw new IOException("Parameter 'ac.baseurl' is required. Please make sure to add this to the `plugin-info` section of the atlassian-plugin.xml");
        }

        String urlPrefix = null != req.getContextPath() ? Paths.get(req.getContextPath(), baseUrl).toString() : baseUrl;
        String urlSuffix = String.format("%s%s%s", moduleUrl, moduleUrl.contains("?") ? "&" : "?", String.join("&", queryStringParameters));

        sb.append("<html>");
        sb.append("<head>");
        params.forEach((k, v) -> sb.append(String.format("<meta name=\"%s\" content=\"%s\">", k, v)));
        sb.append("</head>");
        sb.append("<body>");
        sb.append(String.format("<iframe src=\"%1$s%2$s\" style=\"border:none; overflow: hidden; width: 100%%;\" data-ap-appkey=\"%3$s\" data-ap-key=\"%4$s\"></iframe>", urlPrefix, urlSuffix, plugin.getKey(), moduleKey));
        sb.append("</body>");

        return sb.toString();
    }

}
