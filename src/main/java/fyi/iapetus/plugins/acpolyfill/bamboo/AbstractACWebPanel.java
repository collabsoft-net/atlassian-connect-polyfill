package fyi.iapetus.plugins.acpolyfill.bamboo;

import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.webresource.api.assembler.PageBuilderService;
import fyi.iapetus.plugins.acpolyfill.UserThemeService;
import fyi.iapetus.plugins.acpolyfill.shared.ACWebPanel;

public abstract class AbstractACWebPanel extends ACWebPanel {
    public AbstractACWebPanel(ApplicationProperties applicationProperties, PageBuilderService pageBuilderService, UserManager userManger, UserThemeService userThemeHelper) {
        super(applicationProperties, pageBuilderService, userManger, userThemeHelper);
    }
}
