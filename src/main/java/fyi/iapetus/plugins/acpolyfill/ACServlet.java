package fyi.iapetus.plugins.acpolyfill;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import com.atlassian.sal.api.websudo.WebSudoManager;
import com.atlassian.sal.api.websudo.WebSudoSessionException;
import com.atlassian.upm.api.license.PluginLicenseManager;
import com.atlassian.webresource.api.assembler.*;
import com.google.common.io.Resources;
import com.google.common.base.Strings;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fyi.iapetus.plugins.acpolyfill.shared.PlatformHelper;
import fyi.iapetus.plugins.acpolyfill.shared.PluginHelper;
import fyi.iapetus.plugins.acpolyfill.shared.TemplateRenderer;
import fyi.iapetus.plugins.acpolyfill.shared.UrlHelper;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
@SuppressFBWarnings("SE_BAD_FIELD")
public class ACServlet extends HttpServlet {

    private final PluginHelper pluginHelper;
    private final UrlHelper urlHelper;
    private final TemplateRenderer templateRenderer;
    private final PlatformHelper platformHelper;
    private final WebSudoManager webSudoManager;
    private final LoginUriProvider loginUriProvider;
    private final UserManager userManager;
    private final ApplicationProperties applicationProperties;

    ACServlet(
        @ComponentImport WebSudoManager webSudoManager,
        @ComponentImport LoginUriProvider loginUriProvider,
        @ComponentImport UserManager userManager,
        @ComponentImport ApplicationProperties applicationProperties,
        @ComponentImport PageBuilderService pageBuilderService,
        @ComponentImport PluginAccessor pluginAccessor,
        @ComponentImport PluginLicenseManager pluginLicenseManager
    ) {
        this.pluginHelper = new PluginHelper(pluginAccessor, pluginLicenseManager);
        this.urlHelper = new UrlHelper(this.pluginHelper, applicationProperties);
        this.templateRenderer = new TemplateRenderer(pluginHelper, applicationProperties, userManager, pageBuilderService);
        this.platformHelper = new PlatformHelper(applicationProperties);
        this.webSudoManager = webSudoManager;
        this.loginUriProvider = loginUriProvider;
        this.userManager = userManager;
        this.applicationProperties = applicationProperties;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Check if we have all required permissions
        if (this.isAllowed(req, resp)) {
            String response = "";
            resp.setContentType("text/html; charset=utf-8");

            try (PrintWriter writer = resp.getWriter()) {
                String path = req.getPathInfo();

                if (path.startsWith("/all.js")) {
                    resp.setContentType("application/javascript; charset=utf-8");
                    response = getJavascriptAPI();
                } else {
                    response = templateRenderer.renderAtlassianConnectHost(req);
                }

                writer.write(response);
            } catch (Exception exp) {
                resp.sendError(500, exp.toString());
            }
        }
    }

    private String getJavascriptAPI() throws IOException {
        String platformName = platformHelper.getPlatformName();
        String resourceName = String.format("/ap-%s.js", platformName.toLowerCase());
        return Resources.toString(Resources.getResource(resourceName), StandardCharsets.UTF_8);
    }

    private boolean isAllowed(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();

        // There is no need to check permission for 'all.js'
        if (path.startsWith("/all.js")) {
            return true;
        }

        // Check if the user is logged in
        UserProfile user = userManager.getRemoteUser(req);
        if (user == null) {
            sendRedirectToLogin(req, resp);
            return false;
        }

        // Get the app key from the path
        String appKey = urlHelper.getAppKey(req);

        // Get the plugin from the request object
        Plugin plugin = pluginHelper.getPlugin(appKey);

        // Check if the user has appropriate access based on decorator
        String moduleKey = urlHelper.getModuleKey(req);
        Map<String, String> params = pluginHelper.getModuleParams(plugin, moduleKey);
        String decorator = params.getOrDefault("decorator", "");
        if (decorator.equals("atl.admin")) {
            try {
                webSudoManager.willExecuteWebSudoRequest(req);
            } catch (WebSudoSessionException ignored) {
                webSudoManager.enforceWebSudoProtection(req, resp);
                return false;
            }

            if (!isUserSysAdmin(user)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                return false;
            }
        }

        return true;
    }

    private boolean isUserSysAdmin(UserProfile user) {
        return user != null && userManager.isSystemAdmin(user.getUserKey());
    }

    private void sendRedirectToLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String requestUri = req.getRequestURI();
        String contextPath = req.getContextPath();
        if (!Strings.isNullOrEmpty(contextPath)) {
            requestUri = requestUri.substring(contextPath.length());
        }
        resp.sendRedirect(loginUriProvider.getLoginUri(URI.create(requestUri)).toString());
    }


}
