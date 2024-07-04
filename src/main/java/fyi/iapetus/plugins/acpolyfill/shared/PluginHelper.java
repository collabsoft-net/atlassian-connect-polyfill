package fyi.iapetus.plugins.acpolyfill.shared;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.osgi.bridge.external.PluginRetrievalService;
import com.atlassian.upm.api.license.PluginLicenseManager;
import com.atlassian.upm.api.license.entity.PluginLicense;
import com.atlassian.upm.api.util.Option;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class PluginHelper {

    private final PluginAccessor pluginAccessor;
    private final PluginLicenseManager pluginLicenseManager;
    private PluginRetrievalService pluginRetrievalService;

    public PluginHelper(PluginAccessor pluginAccessor, PluginLicenseManager pluginLicenseManager) {
        this.pluginAccessor = pluginAccessor;
        this.pluginLicenseManager = pluginLicenseManager;
    }

    public PluginHelper(PluginRetrievalService pluginRetrievalService, PluginAccessor pluginAccessor, PluginLicenseManager pluginLicenseManager) {
        this.pluginRetrievalService = pluginRetrievalService;
        this.pluginAccessor = pluginAccessor;
        this.pluginLicenseManager = pluginLicenseManager;
    }

    public Plugin getPlugin() {
        return null != pluginRetrievalService ? pluginRetrievalService.getPlugin() : null;
    }

    public Plugin getPlugin(String appKey) {
        return pluginAccessor.getPlugin(appKey);
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

    public String getLicenseState(Plugin plugin) {
        if(!isLicenseEnabled(plugin)) {
            return "active";
        } else {
            Option<PluginLicense> license = this.pluginLicenseManager.getLicense();
            return (license.isDefined() && license.get().isValid()) ? "active" : "none";
        }
    }

    private boolean isLicenseEnabled(Plugin plugin) {
        Map<String, String> params = plugin
                .getPluginInformation()
                .getParameters();
        return !params.containsKey("atlassian-licensing-enabled") || Boolean.parseBoolean(params.get("atlassian-licensing-enabled"));
    }

}
