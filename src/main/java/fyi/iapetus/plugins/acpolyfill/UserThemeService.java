package fyi.iapetus.plugins.acpolyfill;

import com.atlassian.sal.api.user.UserKey;

import jakarta.annotation.Nonnull;

public interface UserThemeService {
    String getColorMode(@Nonnull UserKey userKey);

}
