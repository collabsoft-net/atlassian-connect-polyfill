package fyi.iapetus.examples;

import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.velocity.htmlsafe.HtmlFragment;
import com.atlassian.sal.api.user.UserKey;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.webresource.api.UrlMode;
import com.atlassian.webresource.api.assembler.WebResourceAssembler;
import com.atlassian.webresource.api.assembler.WebResourceAssemblerFactory;
import com.atlassian.webresource.api.assembler.WebResourceSet;
import com.atlassian.webresource.api.assembler.resource.ResourcePhase;
import fyi.iapetus.plugins.acpolyfill.UserThemeService;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

public class ACServlet extends HttpServlet {

    private final WebResourceAssemblerFactory webResourceAssemblerFactory;
    private final UserManager userManager;
    private final UserThemeService userThemeService;

    ACServlet(@ComponentImport WebResourceAssemblerFactory webResourceAssemblerFactory, @ComponentImport UserManager userManager, @ComponentImport UserThemeService userThemeService) {
        this.webResourceAssemblerFactory = webResourceAssemblerFactory;
        this.userManager = userManager;
        this.userThemeService = userThemeService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/html; charset=utf-8");

        try (PrintWriter writer = resp.getWriter()) {
            String response = renderIframeContent();
            writer.write(response);
        } catch (Exception exp) {
            resp.sendError(500, exp.toString());
        }
    }

    public String renderIframeContent()  throws IOException {
        HtmlFragment resources = this.getResources();

        UserKey userKey = this.userManager.getRemoteUserKey();
        String colorMode = null != userKey ? this.userThemeService.getColorMode(userKey) : "LIGHT";

        return "<!doctype html>" +
        String.format("<html lang=\"en\" data-color-mode=\"%s\" data-theme=\"dark:dark light:light spacing:spacing\">", colorMode.toLowerCase()) +
            "<head>" +
                "<meta charset=\"UTF-8\">" +
                resources +
            "</head>" +
            "<body>" +
            "</body>" +
        "</html>";
    }

    private HtmlFragment getResources() throws IOException {
        Writer writer = new StringWriter();
        WebResourceAssembler assembler = webResourceAssemblerFactory.create().includeSyncbatchResources(false).includeSuperbatchResources(false).autoIncludeFrontendRuntime(false).build();
        assembler.resources().requireWebResource("com.atlassian.auiplugin:split_aui.page.design-tokens-base-themes-css");
        assembler.resources().requireContext(ResourcePhase.DEFER, "confluence-example");
        WebResourceSet resources = assembler.assembled().drainIncludedResources();
        resources.writeHtmlTags(writer, UrlMode.AUTO);

        writer.flush();
        return new HtmlFragment(writer.toString());
    }
}
