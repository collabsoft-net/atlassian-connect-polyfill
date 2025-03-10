package fyi.iapetus.plugins.acpolyfill.bamboo;

import com.atlassian.plugin.spring.scanner.annotation.component.BambooComponent;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.sal.api.user.UserKey;
import fyi.iapetus.plugins.acpolyfill.UserThemeService;

@BambooComponent
@ExportAsService
public class BambooUserThemeService implements UserThemeService {

    @Override
    public String getColorMode(UserKey userKey) {
        // Bamboo does not store the user theme preference in the database
        // Instead they store in client-side in LocalStorage
        // This means that we cannot provide server-side rendering of the color mode
        // We will return the default value (LIGHT) due to the lack of other options
        return "LIGHT";
    }

}
