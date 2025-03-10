package fyi.iapetus.examples;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.user.UserKey;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.velocity.htmlsafe.HtmlFragment;
import com.atlassian.webresource.api.UrlMode;
import com.atlassian.webresource.api.assembler.WebResourceAssembler;
import com.atlassian.webresource.api.assembler.WebResourceAssemblerFactory;
import com.atlassian.webresource.api.assembler.WebResourceSet;
import com.atlassian.webresource.api.assembler.resource.ResourcePhase;
import fyi.iapetus.plugins.acpolyfill.UserThemeService;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

@Component
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
            String response = renderIframeContent(req);
            writer.write(response);
        } catch (Exception exp) {
            resp.sendError(500, exp.toString());
        }
    }

    public String renderIframeContent(HttpServletRequest req)  throws IOException {
        HtmlFragment resources = this.getResources();

        UserKey userKey = this.userManager.getRemoteUserKey();
        String colorMode = null != userKey ? this.userThemeService.getColorMode(userKey) : "LIGHT";

        StringBuilder sb = new StringBuilder();
        sb.append("<!doctype html>");
        sb.append(String.format("<html lang=\"en\" data-color-mode=\"%s\" data-theme=\"dark:dark light:light spacing:spacing\">", colorMode.toLowerCase()));
        sb.append("<head>");
        sb.append("<meta charset=\"UTF-8\">");
        sb.append(new HtmlFragment(resources.toString()));
        sb.append("</head>");
        sb.append("<body>");
        sb.append("</body>");
        sb.append("</html>");
        return sb.toString();
    }

    private HtmlFragment getResources() throws IOException {
        Writer writer = new StringWriter();
        WebResourceAssembler assembler = this.webResourceAssemblerFactory.create().includeSyncbatchResources(false).includeSuperbatchResources(false).autoIncludeFrontendRuntime(false).build();
        assembler.resources().requireWebResource("com.atlassian.auiplugin:split_aui.page.design-tokens-base-themes-css");
        assembler.resources().requireContext(ResourcePhase.DEFER, "bitbucket-example");
        WebResourceSet resources = assembler.assembled().drainIncludedResources();
        resources.writeHtmlTags(writer, UrlMode.AUTO);

        writer.flush();
        return new HtmlFragment(writer.toString());
    }
}
