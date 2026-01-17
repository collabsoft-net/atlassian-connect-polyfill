package fyi.iapetus.plugins.acpolyfill.servlet.filters;

import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.UrlMode;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.websudo.WebSudoManager;
import com.atlassian.sal.api.websudo.WebSudoSessionException;
import fyi.iapetus.plugins.acpolyfill.PluginLicenseRepository;
import fyi.iapetus.plugins.acpolyfill.shared.LicenseHelper;
import fyi.iapetus.plugins.acpolyfill.shared.PluginHelper;
import fyi.iapetus.plugins.acpolyfill.shared.UrlHelper;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Map;

public class ACAuthFilter implements Filter {

    private final String[] WEBSUDO_DECORATORS = {
            "alt.admin"
    };
    private final PluginHelper pluginHelper;
    private final WebSudoManager webSudoManager;
    private final LoginUriProvider loginUriProvider;
    private final UserManager userManager;
    private final ApplicationProperties applicationProperties;

    ACAuthFilter(
            PluginLicenseRepository pluginLicenseRepository,
            @ComponentImport PluginAccessor pluginAccessor,
            @ComponentImport ApplicationProperties applicationProperties,
            @ComponentImport PluginSettingsFactory pluginSettingsFactory,
            @ComponentImport WebSudoManager webSudoManager,
            @ComponentImport LoginUriProvider loginUriProvider,
            @ComponentImport UserManager userManager
    ) {
        LicenseHelper licenseHelper = new LicenseHelper(pluginLicenseRepository, pluginSettingsFactory);
        this.pluginHelper = new PluginHelper(pluginAccessor, new UrlHelper(licenseHelper, applicationProperties));
        this.webSudoManager = webSudoManager;
        this.loginUriProvider = loginUriProvider;
        this.userManager = userManager;
        this.applicationProperties = applicationProperties;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        // Turn the ServletRequest into an HttpServletRequest
        HttpServletRequest req = (HttpServletRequest) servletRequest;
        HttpServletResponse res = (HttpServletResponse) servletResponse;

        // Check if we can allow anonymous access
        if (null == userManager.getRemoteUser(req)) {
            if (!isAvailableForAnonymousAccess(req)) {
                sendRedirectToLogin(req, res);
                return;
            }
        }

        // Check if we need to enforce WebSudo
        if (mustEnforceWebsudo(req)) {
            try {
                webSudoManager.willExecuteWebSudoRequest(req);
            } catch (WebSudoSessionException ignored) {
                webSudoManager.enforceWebSudoProtection(req, res);
                return;
            }
        }

        // Continue as planned
        filterChain.doFilter(servletRequest, servletResponse);
    }

    private boolean isAvailableForAnonymousAccess(HttpServletRequest req) {
        // The JavaScript API is available anonymously
        String path = req.getPathInfo();
        if (null != path && path.toLowerCase().endsWith("/all.js")) {
            return true;
        }

        // Get the parameters for the module
        Map<String, String> params = pluginHelper.getModuleParams(req);

        // Check for the 'ac.AnonymousAccessAllowed' parameter
        String anonymousAccessAllowed = params.getOrDefault("ac.AnonymousAccessAllowed", "false");
        return "true".equalsIgnoreCase(anonymousAccessAllowed);
    }

    private boolean mustEnforceWebsudo(HttpServletRequest req) {
        // Get the parameters for the module
        Map<String, String> params = pluginHelper.getModuleParams(req);

        String decorator = params.getOrDefault("decorator", "");

        // Check if the decorator matches any of the known decorators that require websudo
        if (Arrays.stream(WEBSUDO_DECORATORS).anyMatch(item -> item.equalsIgnoreCase(decorator))) {
            return true;
        }

        // Otherwise, check for the 'ac.WebsudoRequired' parameter
        String websudoRequired = params.getOrDefault("ac.WebsudoRequired", "false");
        return "true".equalsIgnoreCase(websudoRequired);
    }

    private void sendRedirectToLogin(HttpServletRequest req, HttpServletResponse res) throws IOException {
        URI originalUrl = getOriginalUrl(req);
        String loginUrl = loginUriProvider.getLoginUri(originalUrl).toString();
        res.sendRedirect(loginUrl);
    }

    private URI getOriginalUrl(final HttpServletRequest request) {
        String path = null != request.getPathInfo() ? request.getPathInfo() : request.getServletPath();
        String orignalUrl = applicationProperties.getBaseUrl(UrlMode.ABSOLUTE) + path;
        String queryString = request.getQueryString();
        if (null != queryString && !queryString.isEmpty()) {
            orignalUrl = orignalUrl + String.format("?%s", request.getQueryString());
        }
        return URI.create(orignalUrl);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void destroy() {

    }
}
