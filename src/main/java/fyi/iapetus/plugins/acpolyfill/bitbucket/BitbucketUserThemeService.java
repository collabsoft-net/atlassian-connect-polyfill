package fyi.iapetus.plugins.acpolyfill.bitbucket;

import com.atlassian.plugin.spring.scanner.annotation.component.BitbucketComponent;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.BitbucketImport;
import com.atlassian.sal.api.user.UserKey;
import com.atlassian.sal.api.usersettings.UserSettingsService;
import fyi.iapetus.plugins.acpolyfill.UserThemeService;

@BitbucketComponent
@ExportAsService
public class BitbucketUserThemeService implements UserThemeService {

    private static final String PREFERRED_COLOR_MODE_KEY = "theme";

    private final UserSettingsService userSettingsService;

    BitbucketUserThemeService(@BitbucketImport UserSettingsService userSettingsService) {
        this.userSettingsService = userSettingsService;
    }

    @Override
    public String getColorMode(UserKey userKey) {
        return userSettingsService
            .getUserSettings(userKey)
            .getString(PREFERRED_COLOR_MODE_KEY)
            .toOptional()
            .orElse("LIGHT");
    }

}
