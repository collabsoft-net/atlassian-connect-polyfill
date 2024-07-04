package fyi.iapetus.examples;

import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.velocity.htmlsafe.HtmlFragment;
import com.atlassian.webresource.api.UrlMode;
import com.atlassian.webresource.api.assembler.WebResourceAssembler;
import com.atlassian.webresource.api.assembler.WebResourceAssemblerFactory;
import com.atlassian.webresource.api.assembler.WebResourceSet;
import com.atlassian.webresource.api.assembler.resource.ResourcePhase;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

public class ACServlet extends HttpServlet {

    private final WebResourceAssemblerFactory webResourceAssemblerFactory;

    ACServlet(@ComponentImport WebResourceAssemblerFactory webResourceAssemblerFactory) {
        this.webResourceAssemblerFactory = webResourceAssemblerFactory;
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

        StringBuilder sb = new StringBuilder();
        sb.append("<!doctype html>");
        sb.append("<html lang=\"en\">");
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
        WebResourceAssembler assembler = webResourceAssemblerFactory.create().includeSyncbatchResources(false).includeSuperbatchResources(false).autoIncludeFrontendRuntime(false).build();
        assembler.resources().requireContext(ResourcePhase.DEFER, "jira-example");
        WebResourceSet resources = assembler.assembled().drainIncludedResources();
        resources.writeHtmlTags(writer, UrlMode.AUTO);

        writer.flush();
        return new HtmlFragment(writer.toString());
    }
}
