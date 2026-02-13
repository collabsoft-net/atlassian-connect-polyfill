
// This file is commented out because the example plugin does not have licensing enabled
// If you do have licensing enabled for your app, it is recommended to implement the PluginLicenseRepository.register()
// method to ensure that the Atlassian Connect Polyfill servlet can correctly set the license state of the `lic=` parameter

/*
package fyi.iapetus.examples;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.osgi.bridge.external.PluginRetrievalService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.upm.api.license.PluginLicenseManager;
import fyi.iapetus.plugins.acpolyfill.PluginLicenseRepository;
import org.springframework.stereotype.Component;

import jakarta.inject.Inject;

@Component
public class LicenseManager {

    @ComponentImport
    private final PluginLicenseRepository pluginLicenseRepository;
    @ComponentImport
    private final PluginRetrievalService pluginRetrievalService;
    @ComponentImport
    private final PluginLicenseManager pluginLicenseManager;

    @Inject
    public LicenseManager(PluginLicenseRepository pluginLicenseRepository, PluginRetrievalService pluginRetrievalService, PluginLicenseManager pluginLicenseManager) {
        this.pluginLicenseRepository = pluginLicenseRepository;
        this.pluginRetrievalService = pluginRetrievalService;
        this.pluginLicenseManager = pluginLicenseManager;
        registerLicenseManager();
    }

    void registerLicenseManager() {
        Plugin plugin = pluginRetrievalService.getPlugin();
        this.pluginLicenseRepository.register(plugin.getKey(), pluginLicenseManager);
    }

}
*/
