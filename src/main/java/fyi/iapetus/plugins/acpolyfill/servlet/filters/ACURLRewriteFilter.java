package fyi.iapetus.plugins.acpolyfill.servlet.filters;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import fyi.iapetus.plugins.acpolyfill.PluginLicenseRepository;
import fyi.iapetus.plugins.acpolyfill.shared.LicenseHelper;
import fyi.iapetus.plugins.acpolyfill.shared.PluginHelper;
import fyi.iapetus.plugins.acpolyfill.shared.UrlHelper;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;

public class ACURLRewriteFilter implements Filter {

    private final UrlHelper urlHelper;
    private final PluginHelper pluginHelper;

    ACURLRewriteFilter(
            PluginLicenseRepository pluginLicenseRepository,
            @ComponentImport PluginAccessor pluginAccessor,
            @ComponentImport ApplicationProperties applicationProperties,
            @ComponentImport PluginSettingsFactory pluginSettingsFactory
    ) {
        LicenseHelper licenseHelper = new LicenseHelper(pluginLicenseRepository, pluginSettingsFactory);
        this.urlHelper = new UrlHelper(licenseHelper, applicationProperties);
        this.pluginHelper = new PluginHelper(pluginAccessor, urlHelper);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        // Turn the ServletRequest into an HttpServletRequest
        HttpServletRequest req = (HttpServletRequest) servletRequest;
        HttpServletResponse res = (HttpServletResponse) servletResponse;

        // Do not process the webwork action
        if (req.getServletPath().equalsIgnoreCase("/atlassian-connect/AC.action")) {
            filterChain.doFilter(servletRequest, servletResponse);

        // Otherwise, rewrite the URL
        } else {

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

            String[] parameters = {
                    String.format("appkey=%s", urlHelper.getAppKey(req)),
                    String.format("modulekey=%s", urlHelper.getModuleKey(req))
            };

            String[] defaultQueryStringParameters = urlHelper.getDefaultQueryStringParameters(req, plugin);
            String[] queryStringParameters = Arrays.copyOf(defaultQueryStringParameters, defaultQueryStringParameters.length + parameters.length);
            System.arraycopy(parameters, 0, queryStringParameters, defaultQueryStringParameters.length, parameters.length);
            String newUrl = String.format("/atlassian-connect/AC.action?%s", String.join("&", queryStringParameters));

            req.setAttribute("originalUrl", req.getServletPath());
            RequestDispatcher dispatcher = req.getRequestDispatcher(newUrl);
            if (dispatcher != null) {
                dispatcher.forward(req, res);
            }
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void destroy() {

    }
}
