package fyi.iapetus.plugins.acpolyfill.confluence;

import com.atlassian.confluence.user.*;
import com.atlassian.plugin.spring.scanner.annotation.component.ConfluenceComponent;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ConfluenceImport;
import com.atlassian.sal.api.user.UserKey;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fyi.iapetus.plugins.acpolyfill.UserThemeService;

import jakarta.annotation.Nonnull;
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
        UserPreferences confluenceUserPreferences = this.userAccessor.getUserPreferences(user);
        Optional<String> preferredColorMode = confluenceUserPreferences.getString("atl-theme-preferred-color-mode");
        return preferredColorMode.orElse("LIGHT");
    }

}
