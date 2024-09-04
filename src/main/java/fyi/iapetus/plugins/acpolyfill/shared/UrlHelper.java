package fyi.iapetus.plugins.acpolyfill.shared;

import com.atlassian.plugin.Plugin;
import com.atlassian.sal.api.ApplicationProperties;
import org.springframework.util.ReflectionUtils;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class UrlHelper {

    private final PluginHelper pluginHelper;
    private final ApplicationProperties applicationProperties;

    public UrlHelper(PluginHelper pluginHelper, ApplicationProperties applicationProperties) {
        this.pluginHelper = pluginHelper;
        this.applicationProperties = applicationProperties;
    }

    public String getAppKey(HttpServletRequest req) {
        String querystring = getQueryString(req);

        // Support web panels in project sidebar (using `?selectedItem`)
        // This is only applicable to web panels, not servlets
        if (null != querystring && querystring.toLowerCase().contains("selecteditem")) {
            String selectedItem = req.getParameter("selectedItem");
            if (!selectedItem.isEmpty()) {
                String[] parts = selectedItem.split(":");
                return parts[0];
            }

        // Otherwise, just take it from the path
        } else {
            String path = req.getPathInfo();
            if (null != path) {
                String[] pathElements = Arrays.stream(path.split("/")).filter(item -> !item.isEmpty()).toArray(String[]::new);

                if (path.startsWith("/all.js")) {
                    return null;
                } else if (path.startsWith("/embedded")) {
                    return pathElements[1];
                } else {
                    return pathElements[0];
                }
            }
        }

        return null;
    }
    public String getModuleKey(HttpServletRequest req) {
        String path = req.getPathInfo();
        if (null != path) {
            String[] pathElements = Arrays.stream(path.split("/")).filter(item -> !item.isEmpty()).toArray(String[]::new);

            if (path.startsWith("/all.js")) {
                return null;
            } else if (path.startsWith("/embedded")) {
                return pathElements[2];
            } else {
                return pathElements[1];
            }
        }
        return null;
    }

    public String[] getDefaultQueryStringParameters(HttpServletRequest req, Plugin plugin) {
        Map<String, String> parameters = new HashMap<>();

        String baseUrl = this.applicationProperties.getBaseUrl(com.atlassian.sal.api.UrlMode.CANONICAL);
        parameters.put("xdm_e", baseUrl);

        String contextPath = this.applicationProperties.getBaseUrl(com.atlassian.sal.api.UrlMode.RELATIVE_CANONICAL);
        parameters.put("cp", contextPath);
        parameters.put("lic", this.pluginHelper.getLicenseState(plugin));
        parameters.put("xdm_c", "DO_NOT_USE");
        parameters.put("cv", "DO_NOT_USE");

        req.getParameterMap().forEach((key, v) -> {
            if (key.startsWith(("ac."))) {
                String value = req.getParameter(key);
                parameters.put(key.substring(3), URLEncoder.encode(value));
            }
        });

        return parameters
                .entrySet()
                .stream()
                .map(entry -> String.format("%s=%s", entry.getKey(), entry.getValue()))
                .toArray(String[]::new);
    }

    private String getQueryString(HttpServletRequest req) {
        try {
            // Check if the query string object exists (for some reason, it doesn't always exist)
            Method getQueryStringMethod = ReflectionUtils.findMethod(req.getClass(), "getQueryString");
            if (null == getQueryStringMethod) throw new IllegalArgumentException();

            // Check if we can get the value and make sure that the value != null
            return req.getQueryString();
        } catch (Exception ignored) {
            return null;
        }
    }

}
