package fyi.iapetus.plugins.acpolyfill.shared;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.osgi.bridge.external.PluginRetrievalService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@SuppressFBWarnings("EI_EXPOSE_REP2")
public class PluginHelper {

    private final PluginAccessor pluginAccessor;
    private PluginRetrievalService pluginRetrievalService;
    private final UrlHelper urlHelper;

    public PluginHelper(PluginAccessor pluginAccessor, UrlHelper urlHelper) {
        this.pluginAccessor = pluginAccessor;
        this.urlHelper = urlHelper;
    }

    public PluginHelper(PluginRetrievalService pluginRetrievalService, PluginAccessor pluginAccessor, UrlHelper urlHelper) {
        this(pluginAccessor, urlHelper);
        this.pluginRetrievalService = pluginRetrievalService;
    }

    @Nullable
    public Plugin getPlugin() {
        try {
            return null != pluginRetrievalService ? pluginRetrievalService.getPlugin() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    @Nullable
    public Plugin getPlugin(HttpServletRequest req) {
        // Get the plugin from the OSGi bridge
        Plugin plugin = this.getPlugin();

        // If we did not get the plugin, we are in Servlet mode and should retrieve it from the HttpServletRequest
        if (null == plugin) {
            // Get the app key from the path
            String appKey = urlHelper.getAppKey(req);

            // Get the plugin from the request object
            if (null != appKey) {
                plugin = this.getPlugin(appKey);
            }
        }

        // Return the plugin
        return plugin;
    }

    @Nullable
    public Plugin getPlugin(String appKey) {
        try {
            return pluginAccessor.getPlugin(appKey);
        } catch (Exception ignored) {
            return null;
        }
    }

    public Map<String, String> getModuleParams(HttpServletRequest req) {
        // Get the plugin, either from the OSGi bridge or from the request
        Plugin plugin = getPlugin(req);

        // Get the module key
        String moduleKey = urlHelper.getModuleKey(req);

        // If either plugin or module key are null, don't bother
        if (null == plugin || null == moduleKey) {
            return new HashMap<>();
        }

        // Return the module parameters
        return this.getModuleParams(plugin, moduleKey);
    }

    public Map<String, String> getModuleParams(Plugin plugin, String moduleKey) {
        ModuleDescriptor<?> descriptor = this.getModuleDescriptor(plugin, moduleKey);
        if (descriptor != null) {
            return descriptor.getParams();
        } else {
            return new HashMap<>();
        }
    }

    public ModuleDescriptor<?> getModuleDescriptor(Plugin plugin, String moduleKey) {
        ModuleDescriptor<?> descriptor = plugin.getModuleDescriptor(moduleKey);
        if (null != descriptor) return descriptor;
        
        // If we can't find it by key, find it by name!
        return plugin.getModuleDescriptors().stream().filter(item -> {
            String name = null != item.getName() ? item.getName() : "";
            return name.equalsIgnoreCase(moduleKey);
        }).findFirst().orElse(null);
    }

    public String getBaseUrl(Plugin plugin) {
        Map<String, String> params = plugin
                .getPluginInformation()
                .getParameters();
        return params.getOrDefault("ac.baseurl", null);
    }

    public String getContext(Plugin plugin) {
        Map<String, String> params = plugin
                .getPluginInformation()
                .getParameters();
        return params.getOrDefault("ac.context", null);
    }

}
