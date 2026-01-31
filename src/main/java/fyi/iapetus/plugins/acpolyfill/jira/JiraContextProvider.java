package fyi.iapetus.plugins.acpolyfill.jira;

import com.atlassian.jira.plugin.webfragment.contextproviders.AbstractJiraContextProvider;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.PluginParseException;
import fyi.iapetus.plugins.acpolyfill.shared.ContextProviderHelper;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class JiraContextProvider extends AbstractJiraContextProvider {

    private final Map<String, Object> params = new HashMap<>();

    public void init(Map params) throws PluginParseException {
        params.forEach((key, value) -> {
            if (key instanceof String) {
                this.params.put((String)key, value);
            }
        });
    }

    @Override
    public Map getContextMap(ApplicationUser applicationUser, JiraHelper jiraHelper) {
        try {
            HttpServletRequest req = null != jiraHelper ? jiraHelper.getRequest() : null;
            ContextProviderHelper helper = new ContextProviderHelper(req);
            return helper.getContextMap(this.params);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
