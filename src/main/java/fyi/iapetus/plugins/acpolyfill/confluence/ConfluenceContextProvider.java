package fyi.iapetus.plugins.acpolyfill.confluence;

import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.ContextProvider;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fyi.iapetus.plugins.acpolyfill.shared.ContextProviderHelper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static fyi.iapetus.plugins.acpolyfill.shared.OsgiServices.importOsgiService;

public class ConfluenceContextProvider implements ContextProvider {

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
            ContextProviderHelper helper = new ContextProviderHelper();
            return helper.getContextMap(this.params);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
