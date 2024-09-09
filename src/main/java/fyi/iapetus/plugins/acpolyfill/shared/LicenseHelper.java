package fyi.iapetus.plugins.acpolyfill.shared;

import com.atlassian.extras.api.*;
import com.atlassian.extras.core.DefaultAtlassianLicenseFactory;
import com.atlassian.extras.core.DefaultLicenseManager;
import com.atlassian.extras.core.ProductLicenseFactory;
import com.atlassian.extras.core.plugins.PluginLicenseFactory;
import com.atlassian.extras.decoder.api.DelegatingLicenseDecoder;
import com.atlassian.extras.decoder.api.LicenseDecoder;
import com.atlassian.extras.decoder.v1.Version1LicenseDecoder;
import com.atlassian.extras.decoder.v2.Version2LicenseDecoder;
import com.atlassian.plugin.Plugin;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.upm.api.license.PluginLicenseManager;
import com.atlassian.upm.api.license.entity.PluginLicense;
import com.atlassian.upm.api.util.Option;
import fyi.iapetus.plugins.acpolyfill.PluginLicenseRepository;

import java.util.*;

import static fyi.iapetus.plugins.acpolyfill.shared.LongKeyHasher.hashKeyIfTooLong;

public class LicenseHelper {

    private PluginLicenseRepository pluginLicenseRepository;
    private LicenseDecoder decoder;
    private Map<Product, ProductLicenseFactory> productLicenseFactories;
    private LicenseManager licenseManager;
    private PluginLicenseManager pluginLicenseManager;
    private PluginSettings pluginSettings;

    public LicenseHelper(PluginLicenseManager pluginLicenseManager) {
        this.pluginLicenseManager = pluginLicenseManager;
    }

    public LicenseHelper(PluginLicenseRepository pluginLicenseRepository, PluginSettingsFactory pluginSettingsFactory) {
        this.pluginLicenseRepository = pluginLicenseRepository;
        this.decoder = new DelegatingLicenseDecoder(Arrays.asList(new Version2LicenseDecoder(), new Version1LicenseDecoder()));
        this.productLicenseFactories = new HashMap<>();
        this.productLicenseFactories.put(Product.ALL_PLUGINS, new PluginLicenseFactory(Product.ALL_PLUGINS));
        this.pluginSettings = pluginSettingsFactory.createGlobalSettings();
        rebuildLicenseManager();
    }

    // WARNING: here by dragons 🐲
    // The Atlassian Connect Polyfill is a separate plugin, meaning that it has its own license
    // Because the plugin does not support licensing, the pluginLicenseManager will always return null
    // This will result in the license state to be "none", which would indicate that the app is not licensed
    //
    // This only applies to the `servlet` which is part of the separate app.
    // If the `jira` or `confluence` helper packages are used, the code will run within the context of the original app
    // meaning that the PluginLicenseManager will retrieve the license of the app itself, and not the Atlassian Connect
    // Polyfill app
    //
    // In order to correctly determine the license, we will therefore apply a 3-step process:
    //
    // 1. We allow `jira` and `confluence` consumers to pass the PluginLicenseManager through the constructor
    //    If this class is initialised with a PluginLicenseManager, we know that we are running within the app context
    //    and that we can safely determine the license state using the PluginLicenseManager
    //
    // 2. We export a PluginLicenseRepository which will allow apps to register their specific PluginLicenseManager
    //    This is the best way to make sure that we determine the correct license state
    //
    // 3. Finally, we apply a hack in which we retrieve the raw license from the SAL PluginSettings and cast it to a
    //    ProductLicense. This assumes that the key and hashing mechanism doesn't change (code taken from sources).
    //    This is the least reliable method, but we want to make sure that we have a fallback
    public String getLicenseState(Plugin plugin) {
        if(!isLicenseEnabled(plugin)) {
            return "active";
        } else {
            if (null != this.pluginLicenseManager && plugin.getKey().equals(this.pluginLicenseManager.getPluginKey())) {
                Option<PluginLicense> license = this.pluginLicenseManager.getLicense();
                return (null != license && (license.isDefined() && license.get().isValid())) ? "active" : "none";
            }

            if (null != this.pluginLicenseRepository && this.pluginLicenseRepository.has(plugin)) {
                Optional<PluginLicenseManager> licenseManager = this.pluginLicenseRepository.get(plugin);
                if (licenseManager.isPresent()) {
                    PluginLicenseManager manager = licenseManager.get();
                    Option<PluginLicense> license = manager.getLicense();
                    return (null != license && (license.isDefined() && license.get().isValid())) ? "active" : "none";
                }
            }

            if (null != this.licenseManager) {
                Product product = new Product(plugin.getKey(), plugin.getKey(), true);
                String rawLicense = this.getRawLicense(plugin);
                AtlassianLicense atlassianLicense = licenseManager.getLicense(rawLicense);
                ProductLicense license = atlassianLicense.getProductLicense(product);
                boolean isValid = null != license && !license.isExpired();
                return isValid ? "active" : "none";
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

    private String getRawLicense(Plugin plugin) {
        return (String)pluginSettings.get(hashKeyIfTooLong("com.atlassian.upm.license.internal.impl.PluginSettingsPluginLicenseRepository:licenses:" + plugin.getKey()));
    }

    private void rebuildLicenseManager() {
        DefaultAtlassianLicenseFactory defaultAtlassianLicenseFactory = new DefaultAtlassianLicenseFactory(Collections.unmodifiableMap(this.productLicenseFactories));
        this.licenseManager = new DefaultLicenseManager(this.decoder, defaultAtlassianLicenseFactory);
    }
}
