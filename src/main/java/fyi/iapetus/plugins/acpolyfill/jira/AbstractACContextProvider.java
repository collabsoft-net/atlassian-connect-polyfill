package fyi.iapetus.plugins.acpolyfill.jira;

import com.atlassian.jira.plugin.webfragment.contextproviders.AbstractJiraContextProvider;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.osgi.bridge.external.PluginRetrievalService;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.upm.api.license.PluginLicenseManager;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fyi.iapetus.plugins.acpolyfill.shared.ContextProviderHelper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractACContextProvider extends AbstractJiraContextProvider {

    private final PluginLicenseManager pluginLicenseManager;
    private final PluginRetrievalService pluginRetrievalService;
    private final PluginAccessor pluginAccessor;
    private final ApplicationProperties applicationProperties;

    public AbstractACContextProvider(
        PluginLicenseManager pluginLicenseManager,
        PluginRetrievalService pluginRetrievalService,
        PluginAccessor pluginAccessor,
        ApplicationProperties applicationProperties
    ) {
        this.pluginLicenseManager = pluginLicenseManager;
        this.pluginRetrievalService = pluginRetrievalService;
        this.pluginAccessor = pluginAccessor;
        this.applicationProperties = applicationProperties;
    }

    private final Map<String, Object> params = new HashMap<>();

    public void init(Map params) throws PluginParseException {
        params.forEach((key, value) -> {
            if (key instanceof String) {
                this.params.put((String)key, value);
            }
        });
    }

    @Override
    @SuppressFBWarnings("THROWS_METHOD_THROWS_RUNTIMEEXCEPTION")
    public Map getContextMap(ApplicationUser applicationUser, JiraHelper jiraHelper) {
        try {
            ContextProviderHelper helper = new ContextProviderHelper(jiraHelper.getRequest(), pluginLicenseManager, pluginRetrievalService, pluginAccessor, applicationProperties);
            return helper.getContextMap(this.params);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
