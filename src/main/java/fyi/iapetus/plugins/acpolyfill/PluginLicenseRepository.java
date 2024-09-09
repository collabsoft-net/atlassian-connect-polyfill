package fyi.iapetus.plugins.acpolyfill;

import com.atlassian.plugin.Plugin;
import com.atlassian.upm.api.license.PluginLicenseManager;
import com.atlassian.upm.api.license.entity.PluginLicense;

import java.util.Optional;

public interface PluginLicenseRepository {
    void register(Plugin plugin, PluginLicenseManager license);
    void register(String pluginKey, PluginLicenseManager license);
    void unregister(Plugin plugin);
    void unregister(String pluginKey);
    boolean has(Plugin plugin);
    boolean has(String pluginKey);
    Optional<PluginLicenseManager> get(Plugin plugin);
    Optional<PluginLicenseManager> get(String pluginKey);

}
