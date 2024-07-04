package fyi.iapetus.examples;

import com.atlassian.confluence.web.context.HttpContext;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.osgi.bridge.external.PluginRetrievalService;
import com.atlassian.plugin.spring.scanner.annotation.component.ConfluenceComponent;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.upm.api.license.PluginLicenseManager;
import com.atlassian.webresource.api.assembler.PageBuilderService;
import fyi.iapetus.plugins.acpolyfill.confluence.AbstractACMacro;
import org.springframework.beans.factory.annotation.Autowired;

@ConfluenceComponent
public class ACMacro extends AbstractACMacro {

    @Autowired
    public ACMacro(
            @ComponentImport HttpContext httpContext,
            @ComponentImport PluginLicenseManager pluginLicenseManager,
            @ComponentImport UserManager userManager,
            @ComponentImport PluginRetrievalService pluginRetrievalService,
            @ComponentImport PluginAccessor pluginAccessor,
            @ComponentImport ApplicationProperties applicationProperties,
            @ComponentImport PageBuilderService pageBuilderService
    ) {
        super(httpContext, pluginLicenseManager, userManager, pluginRetrievalService, pluginAccessor, applicationProperties, pageBuilderService);
    }

}