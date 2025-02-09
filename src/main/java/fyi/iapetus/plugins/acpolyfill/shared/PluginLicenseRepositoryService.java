package fyi.iapetus.plugins.acpolyfill.shared;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.upm.api.license.PluginLicenseManager;
import fyi.iapetus.plugins.acpolyfill.PluginLicenseRepository;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
@ExportAsService
public class PluginLicenseRepositoryService implements PluginLicenseRepository {

    private final Map<String, PluginLicenseManager> licenses;

    public PluginLicenseRepositoryService() {
        this.licenses = new HashMap<>();
    }

    public void register(Plugin plugin, PluginLicenseManager licenseManager) {
        this.register(plugin.getKey(), licenseManager);
    }

    public void register(String pluginKey, PluginLicenseManager licenseManager) {
        this.licenses.put(pluginKey, licenseManager);
    }

    public void unregister(Plugin plugin) {
        unregister(plugin.getKey());
    }

    public void unregister(String pluginKey) {
        licenses.remove(pluginKey);
    }

    public boolean has(Plugin plugin) {
        return this.has(plugin.getKey());
    }

    public boolean has(String pluginKey) {
        return this.licenses.containsKey(pluginKey);
    }

    public Optional<PluginLicenseManager> get(Plugin plugin) {
        return this.get(plugin.getKey());
    }

    public Optional<PluginLicenseManager> get(String pluginKey) {
        return Optional.ofNullable(licenses.getOrDefault(pluginKey, null));
    }

}
