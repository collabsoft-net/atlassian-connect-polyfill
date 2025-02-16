package fyi.iapetus.plugins.acpolyfill.shared;

import com.atlassian.plugin.Plugin;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.upm.api.license.PluginLicenseManager;
import com.atlassian.upm.api.license.entity.PluginLicense;
import com.atlassian.upm.api.util.Option;
import fyi.iapetus.plugins.acpolyfill.PluginLicenseRepository;

import java.util.*;

// WARNING: here by dragons 🐲
// The Atlassian Connect Polyfill is a separate plugin, meaning that it has its own license
// Because the plugin does not support licensing, the pluginLicenseManager will always return null
// This will result in the license state to be "none", which would indicate that the app is not licensed
//
// In order to support licensing when using the Atlassian Connect Polyfill Servlet
// apps need to import the PluginLicenseManager and register their PluginLicenseManager.
// This will allow the Servlet to correctly detect the license state.
//
// If the `jira`, `confluence`, 'bamboo' and 'bitbucket' host helper packages are used, the code will run within the
// context of the original app, meaning that the PluginLicenseManager will retrieve the license of the app itself,
// and not the Atlassian Connect Polyfill app.
//
// In this case, the applicable helper classes will use OSGi imports to get the PluginLicenseManager
// so that we can safely determine the license state using the PluginLicenseManager
public class LicenseHelper {

    private PluginLicenseRepository pluginLicenseRepository;
    private PluginLicenseManager pluginLicenseManager;

    public LicenseHelper(PluginLicenseManager pluginLicenseManager) {
        this.pluginLicenseManager = pluginLicenseManager;
    }

    public LicenseHelper(PluginLicenseRepository pluginLicenseRepository, PluginSettingsFactory pluginSettingsFactory) {
        this.pluginLicenseRepository = pluginLicenseRepository;
    }

    public String getLicenseState(Plugin plugin) {
        if(!isLicenseEnabled(plugin)) {
            return "active";
        } else {
            if (null != this.pluginLicenseManager && plugin.getKey().equals(this.pluginLicenseManager.getPluginKey())) {
                try {
                    Option<PluginLicense> license = this.pluginLicenseManager.getLicense();
                    return (null != license && (license.isDefined() && license.get().isValid())) ? "active" : "none";
                } catch (Exception _ignored) {
                    // If an exception is thrown, assume that the license isn't valid
                }
            }

            if (null != this.pluginLicenseRepository && this.pluginLicenseRepository.has(plugin)) {
                try {
                    Optional<PluginLicenseManager> licenseManager = this.pluginLicenseRepository.get(plugin);
                    if (licenseManager.isPresent()) {
                        PluginLicenseManager manager = licenseManager.get();
                        Option<PluginLicense> license = manager.getLicense();
                        return (null != license && (license.isDefined() && license.get().isValid())) ? "active" : "none";
                    }
                } catch (Exception _ignored) {
                    // If an exception is thrown, assume that the license isn't valid
                }
            }

            return "none";
        }
    }

    private boolean isLicenseEnabled(Plugin plugin) {
        Map<String, String> params = plugin
                .getPluginInformation()
                .getParameters();
        return !params.containsKey("atlassian-licensing-enabled") || Boolean.parseBoolean(params.get("atlassian-licensing-enabled"));
    }

}
