package fyi.iapetus.plugins.acpolyfill.confluence;

import com.atlassian.confluence.user.*;
import com.atlassian.core.user.preferences.UserPreferences;
import com.atlassian.plugin.spring.scanner.annotation.component.ConfluenceComponent;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ConfluenceImport;
import com.atlassian.sal.api.user.UserKey;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fyi.iapetus.plugins.acpolyfill.UserThemeService;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

@ConfluenceComponent
@ExportAsService
@SuppressFBWarnings("EI_EXPOSE_REP2")
public class ConfluenceUserThemeService implements UserThemeService {

    private final UserAccessor userAccessor;

    public ConfluenceUserThemeService(@ConfluenceImport UserAccessor userAccessor) {
        this.userAccessor = userAccessor;
    }

    @Override
    public String getColorMode(@Nonnull UserKey userKey) {
        ConfluenceUser user = this.userAccessor.getUserByKey(userKey);
        ConfluenceUserPreferences userPreferences = this.userAccessor.getConfluenceUserPreferences(user);

        // ConfluenceUserPreferences signature changes between different versions of Confluence
        // In the most recent versions of Confluence, the ConfluenceUserPreferences has a direct getString()
        // method which returns an Optional<String>. Let's see if this is the case for the current implementation
        try {
            Method getStringMethod = userPreferences.getClass().getMethod("getString", String.class);
            Object result = getStringMethod.invoke(this, "atl-theme-preferred-color-mode");
            if (result instanceof Optional) {
                Object colorMode = ((Optional<?>) result).isPresent() ? ((Optional<?>) result).get() : "LIGHT";
                if (colorMode instanceof String) {
                    return (String) colorMode;
                }
            }
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | InvocationTargetException ignored) {}

        // In earlier versions of Confluence, the ConfluenceUserPreferences is a wrapper around UserPreferences
        // We need to get the UserPreferences by invoking `getWrappedPreferences` and then get the preference
        try {
            Method userPreferencesMethod = userPreferences.getClass().getMethod("getWrappedPreferences");
            Object result = userPreferencesMethod.invoke(this);
            if (result instanceof UserPreferences) {
                String colorMode = ((UserPreferences) result).getString("atl-theme-preferred-color-mode");
                return null != colorMode ? colorMode : "LIGHT";
            }
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | InvocationTargetException ignored) {}

        // This should not happen, but in case it does, return the default value
        return "LIGHT";
    }

}
