package fyi.iapetus.examples;

import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.webresource.api.assembler.PageBuilderService;
import fyi.iapetus.plugins.acpolyfill.jira.AbstractACWebPanel;

public class ACWebPanel extends AbstractACWebPanel {

    ACWebPanel(@ComponentImport ApplicationProperties applicationProperties, @ComponentImport PageBuilderService pageBuilderService, @ComponentImport UserManager userManger) {
        super(applicationProperties, pageBuilderService, userManger);
    }
}
