package fyi.iapetus.plugins.acpolyfill.confluence;

import com.atlassian.confluence.web.context.HttpContext;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.osgi.bridge.external.PluginRetrievalService;
import com.atlassian.plugin.web.ContextProvider;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.upm.api.license.PluginLicenseManager;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fyi.iapetus.plugins.acpolyfill.shared.ContextProviderHelper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractACContextProvider implements ContextProvider {

    private final HttpContext httpContext;
    private final PluginLicenseManager pluginLicenseManager;
    private final PluginRetrievalService pluginRetrievalService;
    private final PluginAccessor pluginAccessor;
    private final ApplicationProperties applicationProperties;

    public AbstractACContextProvider(
            HttpContext httpContext,
            PluginLicenseManager pluginLicenseManager,
            PluginRetrievalService pluginRetrievalService,
            PluginAccessor pluginAccessor,
            ApplicationProperties applicationProperties
    ) {
        this.httpContext = httpContext;
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
    public Map getContextMap(Map<String, Object> context) {
        try {
            ContextProviderHelper helper = new ContextProviderHelper(httpContext.getRequest(), pluginLicenseManager, pluginRetrievalService, pluginAccessor, applicationProperties);
            return helper.getContextMap(this.params);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
