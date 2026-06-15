package framework;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;

public class FrontControllerServlet extends HttpServlet {

    protected void processRequest(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String uri = req.getRequestURI();
        String contextPath = req.getContextPath();
        String chemin = uri.substring(contextPath.length());

        LinkCollector.add(chemin);
        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();

        out.println("<!DOCTYPE html>");
        out.println("<html><head><title>Framework - URLs capturées</title></head><body>");
        out.println("<h1>URL demandée : " + chemin + "</h1>");
        out.println("<h2>Toutes les URLs capturées :</h2>");
        out.println("<ul>");

        Set<String> liens = LinkCollector.getAll();
        for (String lien : liens) {
            out.println("<li>" + lien + "</li>");
        }

        out.println("</ul>");
        out.println("<p>Total : " + liens.size() + " URL(s)</p>");
        out.println("</body></html>");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        processRequest(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        processRequest(req, resp);
    }
}