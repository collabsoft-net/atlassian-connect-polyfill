package fyi.iapetus.plugins.acpolyfill.confluence;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.macro.Macro;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.macro.browser.beans.MacroFormDetails;
import com.atlassian.confluence.macro.browser.beans.MacroMetadata;
import com.atlassian.confluence.macro.browser.beans.MacroParameter;
import com.atlassian.confluence.renderer.PageContext;
import com.atlassian.confluence.web.context.HttpContext;
import com.atlassian.confluence.xhtml.api.MacroDefinition;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.osgi.bridge.external.PluginRetrievalService;
import com.atlassian.renderer.RenderContext;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.upm.api.license.PluginLicenseManager;
import com.atlassian.webresource.api.assembler.PageBuilderService;
import com.atlassian.webresource.api.assembler.WebResourceAssemblerFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fyi.iapetus.plugins.acpolyfill.UserThemeService;
import fyi.iapetus.plugins.acpolyfill.shared.LicenseHelper;
import fyi.iapetus.plugins.acpolyfill.shared.PluginHelper;
import fyi.iapetus.plugins.acpolyfill.shared.TemplateRenderer;
import fyi.iapetus.plugins.acpolyfill.shared.UrlHelper;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;

@SuppressFBWarnings("EI_EXPOSE_REP2")
public abstract class AbstractACMacro implements Macro {

    private final PluginHelper pluginHelper;
    private final TemplateRenderer templateRenderer;
    private final HttpContext httpContext;

    public AbstractACMacro(
            HttpContext httpContext,
            PluginLicenseManager pluginLicenseManager,
            UserManager userManager,
            PluginRetrievalService pluginRetrievalService,
            PluginAccessor pluginAccessor,
            ApplicationProperties applicationProperties,
            PageBuilderService pageBuilderService,
            WebResourceAssemblerFactory webResourceAssemblerFactory,
            UserThemeService userThemeHelper
    ) {
        LicenseHelper licenseHelper = new LicenseHelper(pluginLicenseManager);
        UrlHelper urlHelper = new UrlHelper(licenseHelper, applicationProperties);
        this.pluginHelper = new PluginHelper(pluginRetrievalService, pluginAccessor, urlHelper);
        this.templateRenderer = new TemplateRenderer(licenseHelper, pluginHelper, applicationProperties, userManager, pageBuilderService, webResourceAssemblerFactory, userThemeHelper);
        this.httpContext = httpContext;
    }

    public String execute(Map<String, String> params, String s, ConversionContext conversionContext) throws MacroExecutionException {
        try {
            HttpServletRequest req = this.httpContext.getRequest();

            // Add the default value of a parameters to the params list (if missing)
            MacroMetadata metadata = conversionContext.hasProperty("macroMetadata") ? (MacroMetadata) conversionContext.getProperty("macroMetadata") : null;
            if (null != metadata) {
                MacroFormDetails formDetails = metadata.getFormDetails();
                if (null != formDetails) {
                    List<MacroParameter> macroParameters = formDetails.getParameters()
                            .stream()
                            .filter(item -> !params.containsKey(item.getName()) && !(null == item.getDefaultValue() || item.getDefaultValue().isEmpty()))
                            .collect(Collectors.toList());
                    macroParameters.forEach(item -> params.put(item.getName(), URLEncoder.encode(item.getDefaultValue())));
                }
            }

            // Add the page ID to the params list
            RenderContext pageContext = conversionContext.hasProperty("renderContext") ? (RenderContext) conversionContext.getProperty("renderContext") : null;
            if (pageContext instanceof PageContext) {
                String refId = ((PageContext) pageContext).getEntity().getContentId().toString();
                params.put("refId", refId);
            }

            // Add the macro ID to the params list
            MacroDefinition macroDefinition = conversionContext.hasProperty("macroDefinition") ? (MacroDefinition) conversionContext.getProperty("macroDefinition") : null;
            if (null != macroDefinition) {
                macroDefinition.getMacroIdentifier().ifPresent(macroId -> params.put("entityId", macroId.getId()));
            }

            String moduleName = conversionContext.hasProperty("macroName") ? conversionContext.getPropertyAsString("macroName") : null;
            String moduleId = params.getOrDefault("entityId", "");

            // This is not the correct module key, but there is no way to get this from the macro definition
            // We will use the module name or ID as a placeholder, until we have found the descriptor
            String moduleKey = null != moduleName ? moduleName : moduleId;

            // Now try to find the descriptor based on the module name
            // If we can find the descriptor based on the name, we can get the correct module key
            if (null != moduleName) {
                ModuleDescriptor<?> descriptor = pluginHelper.getModuleDescriptor(pluginHelper.getPlugin(), moduleName);
                if (null != descriptor) {
                    moduleKey = descriptor.getKey();
                }
            }

            return templateRenderer.renderAtlassianConnectHost(req, moduleKey, getMacroParameters(req, params));
        } catch (IOException | URISyntaxException e) {
            throw new MacroExecutionException(e);
        }
    }

    public BodyType getBodyType() { return BodyType.NONE; }

    public OutputType getOutputType() { return OutputType.BLOCK; }

    private String[] getMacroParameters(HttpServletRequest req, Map<String, String> macroData) {
        // Add the macro parameters to the query string
        // However we need to filter out the ": = | RAW | = :" property
        return macroData
                .entrySet()
                .stream()
                .filter((entry) -> !entry.getKey().equals(": = | RAW | = :"))
                .map(entry -> String.format("%s=%s", entry.getKey(), URLEncoder.encode(entry.getValue())))
                .toArray(String[]::new);
    }

}