package fyi.iapetus.plugins.acpolyfill.shared;

import com.atlassian.plugin.Plugin;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.UrlMode;
import org.springframework.util.ReflectionUtils;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class UrlHelper {

    private final LicenseHelper licenseHelper;
    private final ApplicationProperties applicationProperties;

    public UrlHelper(LicenseHelper licenseHelper, ApplicationProperties applicationProperties) {
        this.licenseHelper = licenseHelper;
        this.applicationProperties = applicationProperties;
    }

    public String getBaseUrl() {
        return this.applicationProperties.getBaseUrl(com.atlassian.sal.api.UrlMode.CANONICAL);
    }

    public String getContextPath() {
        return this.applicationProperties.getBaseUrl(UrlMode.RELATIVE_CANONICAL);
    }

    @Nullable
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

        // Support WebWork Action (using `?appkey=`)
        } else if (null != querystring && querystring.toLowerCase().contains("appkey")) {
            String appKey = req.getParameter("appkey");
            if (!appKey.isEmpty()) {
                return appKey;
            }

        // Otherwise, just take it from the path
        } else {
            String path = null != req.getPathInfo()
                ? req.getPathInfo()
                : req.getServletPath();

            if (null != path) {
                // Strip the "/atlassian-connect" prefix if present
                if (path.startsWith("/atlassian-connect")) {
                    path = path.replace("/atlassian-connect", "");
                }

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
    @Nullable
    public String getModuleKey(HttpServletRequest req) {
        String querystring = getQueryString(req);

        // Support WebWork Action (using `?modulekey=`)
        if (null != querystring && querystring.toLowerCase().contains("modulekey")) {
            String appKey = req.getParameter("modulekey");
            if (!appKey.isEmpty()) {
                return appKey;
            }

        // Otherwise, just take it from the path
        } else {
            String path = null != req.getPathInfo()
                    ? req.getPathInfo()
                    : req.getServletPath();
            ;
            if (null != path) {
                // Strip the "/atlassian-connect" prefix if present
                if (path.startsWith("/atlassian-connect")) {
                    path = path.replace("/atlassian-connect", "");
                }

                String[] pathElements = Arrays.stream(path.split("/")).filter(item -> !item.isEmpty()).toArray(String[]::new);

                if (path.startsWith("/all.js")) {
                    return null;
                } else if (path.startsWith("/embedded")) {
                    return pathElements[2];
                } else {
                    return pathElements[1];
                }
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
        parameters.put("lic", this.licenseHelper.getLicenseState(plugin));
        parameters.put("xdm_c", "DO_NOT_USE");
        parameters.put("cv", "DO_NOT_USE");
        parameters.putAll(this.getACQueryStringParameters(req));

        return parameters
                .entrySet()
                .stream()
                .map(entry -> String.format("%s=%s", entry.getKey(), entry.getValue()))
                .toArray(String[]::new);
    }

    public Map<String, String> getACQueryStringParameters(HttpServletRequest req) {
        Map<String, String> parameters = new HashMap<>();

        req.getParameterMap().forEach((key, v) -> {
            if (key.startsWith(("ac."))) {
                String value = req.getParameter(key);
                parameters.put(key.substring(3), URLEncoder.encode(value));
            }
        });

        return parameters;
    }

    @Nullable
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
