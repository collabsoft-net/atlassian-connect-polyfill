package fyi.iapetus.plugins.acpolyfill.jira;

import com.atlassian.jira.user.preferences.ExtendedPreferences;
import com.atlassian.jira.user.preferences.UserPreferencesManager;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.plugin.spring.scanner.annotation.component.JiraComponent;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.atlassian.sal.api.user.UserKey;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fyi.iapetus.plugins.acpolyfill.UserThemeService;

import jakarta.annotation.Nonnull;
import java.util.Objects;

@JiraComponent
@ExportAsService
@SuppressFBWarnings("EI_EXPOSE_REP2")
public class JiraUserThemeService implements UserThemeService {

    private final UserPreferencesManager userPreferencesManager;
    private final UserManager userManager;

    public JiraUserThemeService(
            @JiraImport UserPreferencesManager userPreferencesManager,
            @JiraImport UserManager userManager
    ) {
        this.userPreferencesManager = userPreferencesManager;
        this.userManager = userManager;
    }

    @Override
    public String getColorMode(UserKey userKey) {
        try {
            ExtendedPreferences preferences = this.getUserPreferences(userKey);
            if (preferences.containsValue("atl-theme-preferred-color-mode")) {
                return this.getUserPreferences(userKey).getString("atl-theme-preferred-color-mode");
            } else {
                return "LIGHT";
            }
        } catch (Exception ignored) {
            return "LIGHT";
        }
    }

    private ExtendedPreferences getUserPreferences(@Nonnull UserKey userKey) {
        Objects.requireNonNull(userKey, "userKey");
        return this.userPreferencesManager.getExtendedPreferences(this.userManager.getUserByKey(userKey.getStringValue()));
    }

}
