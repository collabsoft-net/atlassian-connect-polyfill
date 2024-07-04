package fyi.iapetus.examples;

import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.upm.api.license.PluginLicenseManager;
import fyi.iapetus.plugins.acpolyfill.jira.AbstractACContextProvider;

public class ACContextProvider extends AbstractACContextProvider {

    ACContextProvider(@ComponentImport PluginAccessor pluginAccessor, @ComponentImport ApplicationProperties applicationProperties, @ComponentImport PluginLicenseManager pluginLicenseManager) {
        super(pluginAccessor, applicationProperties, pluginLicenseManager);
    }

}
