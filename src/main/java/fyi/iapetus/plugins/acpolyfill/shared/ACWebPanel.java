package fyi.iapetus.plugins.acpolyfill.shared;

import com.atlassian.plugin.web.model.WebPanel;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.webresource.api.assembler.PageBuilderService;
import com.atlassian.webresource.api.assembler.resource.ResourcePhase;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fyi.iapetus.plugins.acpolyfill.UserThemeService;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;


@SuppressFBWarnings("EI_EXPOSE_REP2")
public abstract class ACWebPanel implements WebPanel {

    private final PlatformHelper platformHelper;
    private final ApplicationProperties applicationProperties;
    private final PageBuilderService pageBuilderService;
    private final UserManager userManager;
    private final UserThemeService userThemeHelper;

    public ACWebPanel(
        ApplicationProperties applicationProperties,
        PageBuilderService pageBuilderService,
        UserManager userManger,
        UserThemeService userThemeHelper
    ) {
        this.platformHelper = new PlatformHelper(applicationProperties);
        this.applicationProperties = applicationProperties;
        this.pageBuilderService = pageBuilderService;
        this.userManager = userManger;
        this.userThemeHelper = userThemeHelper;
    }

    @Override
    public void writeHtml(Writer writer, Map<String, Object> context) throws IOException {
        writer.write(getHtml(context));
    }

    @Override
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
                if (!key.startsWith("ac")) {
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
