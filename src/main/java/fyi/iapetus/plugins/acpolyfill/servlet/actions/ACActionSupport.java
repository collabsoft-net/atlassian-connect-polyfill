package fyi.iapetus.plugins.acpolyfill.servlet.actions;

import com.atlassian.bamboo.FeatureManager;
import com.atlassian.bamboo.project.Project;
import com.atlassian.bamboo.project.ProjectManager;
import com.atlassian.bamboo.security.BambooPermissionManager;
import com.atlassian.bamboo.ww2.BambooActionSupport;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.spring.scanner.annotation.imports.BambooImport;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.webresource.api.assembler.PageBuilderService;
import com.atlassian.webresource.api.assembler.WebResourceAssemblerFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fyi.iapetus.plugins.acpolyfill.PluginLicenseRepository;
import fyi.iapetus.plugins.acpolyfill.UserThemeService;
import fyi.iapetus.plugins.acpolyfill.shared.*;
import org.apache.struts2.ServletActionContext;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@SuppressFBWarnings({ "SE_BAD_FIELD", "EI_EXPOSE_REP" })
public class ACActionSupport extends BambooActionSupport {

    private String template;
    private String projectKey;
    private Project project;

    private final TemplateRenderer templateRenderer;

    @Inject
    ACActionSupport(
            PluginLicenseRepository pluginLicenseRepository,
            UserThemeService userThemeService,
            @ComponentImport UserManager userManager,
            @ComponentImport ApplicationProperties applicationProperties,
            @ComponentImport PageBuilderService pageBuilderService,
            @ComponentImport PluginAccessor pluginAccessor,
            @ComponentImport PluginSettingsFactory pluginSettingsFactory,
            @ComponentImport WebResourceAssemblerFactory webResourceAssemblerFactory,
            @BambooImport ProjectManager projectManager,
            @BambooImport FeatureManager featureManager,
            @BambooImport BambooPermissionManager bambooPermissionManager
    ) {
        LicenseHelper licenseHelper = new LicenseHelper(pluginLicenseRepository, pluginSettingsFactory);
        UrlHelper urlHelper = new UrlHelper(licenseHelper, applicationProperties);
        PluginHelper pluginHelper = new PluginHelper(pluginAccessor, urlHelper);
        this.templateRenderer = new TemplateRenderer(licenseHelper, pluginHelper, applicationProperties, userManager, pageBuilderService, webResourceAssemblerFactory, userThemeService);
        this.projectManager = projectManager;
        this.featureManager = featureManager;
        this.bambooPermissionManager = bambooPermissionManager;
    }

    public String getTemplate() {
        return this.template;
    }

    public @Nullable Project getProject() {
        if (null == this.project) {
            if (null != this.projectKey) {
                this.project = projectManager.getProjectByKey(projectKey);
            }
        }
        return this.project;
    }

    public String getProjectKey() {
        return this.projectKey;
    }

    public void setProjectKey(String projectKey) {
        this.projectKey = projectKey;
    }

    public boolean isProjectWithPlans() {
        List<Project> emptyProjects = projectManager.getEmptyProjects();
        return !emptyProjects.contains(project);
    }

    @Override
    public String execute() throws Exception {
        HttpServletRequest req = ServletActionContext.getRequest();;
        this.template = templateRenderer.renderAtlassianConnectHost(req, TemplateRenderer.RenderType.EMBEDDED);
        return SUCCESS;
    }
}
