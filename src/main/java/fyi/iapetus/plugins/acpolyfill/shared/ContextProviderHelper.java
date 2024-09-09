package fyi.iapetus.plugins.acpolyfill.shared;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.osgi.bridge.external.PluginRetrievalService;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.upm.api.license.PluginLicenseManager;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ContextProviderHelper {

    private final HttpServletRequest httpServletRequest;
    private final UrlHelper urlHelper;
    private final PluginHelper pluginHelper;

    public ContextProviderHelper(
            HttpServletRequest httpServletRequest,
            PluginLicenseManager pluginLicenseManager,
            PluginRetrievalService pluginRetrievalService,
            PluginAccessor pluginAccessor,
            ApplicationProperties applicationProperties
    ) {
        this.httpServletRequest = httpServletRequest;
        this.pluginHelper = new PluginHelper(pluginRetrievalService, pluginAccessor);
        this.urlHelper = new UrlHelper(new LicenseHelper(pluginLicenseManager), applicationProperties);
    }

    public Map getContextMap(Map<String, Object> params) throws IOException {
        // Get the plugin from the OSGi bridge
        Plugin plugin = pluginHelper.getPlugin();

        // It should not be possible that the plugin is unavailable through the OSGi bridge
        // However, if this is the case, check if we can find the plugin through the app key
        if (null == plugin) {
            // Check if the app key is part of the context provider parameters
            String appKey = (String) params.getOrDefault("ac.plugin.key", null);
            if (null == appKey) {
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
        if (null == moduleKey) {
            moduleKey = urlHelper.getModuleKey(httpServletRequest);
        }

        // Make sure that the module key is available
        if (null == moduleKey) {
            throw new IllegalArgumentException("Failed to determine module. Please provide 'ac.moduleKey' as parameter of the context provider in atlassian-plugin.xml");
        }

        params.put("ac.plugin.key", plugin.getKey());
        params.put("ac.plugin.version", plugin.getPluginInformation().getVersion());
        params.put("ac.queryString", String.join("&", urlHelper.getDefaultQueryStringParameters(httpServletRequest, plugin)));
        params.put("ac.moduleKey", moduleKey);

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

        return new HashMap<>(params);
    }
}
