package fyi.iapetus.plugins.acpolyfill.servlet;

import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.webresource.api.assembler.*;
import com.google.common.io.Resources;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fyi.iapetus.plugins.acpolyfill.PluginLicenseRepository;
import fyi.iapetus.plugins.acpolyfill.UserThemeService;
import fyi.iapetus.plugins.acpolyfill.shared.*;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

@Component
@SuppressFBWarnings("SE_BAD_FIELD")
public class ACServlet extends HttpServlet {

    private final TemplateRenderer templateRenderer;
    private final PlatformHelper platformHelper;

    ACServlet(
        PluginLicenseRepository pluginLicenseRepository,
        UserThemeService userThemeService,
        @ComponentImport UserManager userManager,
        @ComponentImport ApplicationProperties applicationProperties,
        @ComponentImport PageBuilderService pageBuilderService,
        @ComponentImport PluginAccessor pluginAccessor,
        @ComponentImport PluginSettingsFactory pluginSettingsFactory,
        @ComponentImport WebResourceAssemblerFactory webResourceAssemblerFactory
    ) {
        LicenseHelper licenseHelper = new LicenseHelper(pluginLicenseRepository, pluginSettingsFactory);
        UrlHelper urlHelper = new UrlHelper(licenseHelper, applicationProperties);
        this.templateRenderer = new TemplateRenderer(licenseHelper, new PluginHelper(pluginAccessor, urlHelper), applicationProperties, userManager, pageBuilderService, webResourceAssemblerFactory, userThemeService);
        this.platformHelper = new PlatformHelper(applicationProperties);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String response = "";
        resp.setContentType("text/html; charset=utf-8");

        try (PrintWriter writer = resp.getWriter()) {
            String path = req.getPathInfo();

            if (path.startsWith("/all.js")) {
                resp.setContentType("application/javascript; charset=utf-8");
                response = getJavascriptAPI();
            } else if (path.startsWith("/embedded")) {
                response = templateRenderer.renderAtlassianConnectHost(req, TemplateRenderer.RenderType.EMBEDDED);
            } else {
                response = templateRenderer.renderAtlassianConnectHost(req);
            }

            writer.write(response);
        } catch (Exception exp) {
            resp.sendError(500, exp.toString());
        }
    }

    private String getJavascriptAPI() throws IOException {
        String platformName = platformHelper.getPlatformName();
        String resourceName = String.format("/ap-%s.js", platformName.toLowerCase());
        return Resources.toString(Resources.getResource(resourceName), StandardCharsets.UTF_8);
    }




}
