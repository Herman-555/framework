package mg.itu.Servlet;

import mg.itu.annotation.Controller.Controller;
import mg.itu.annotation.Url.UrlMapping;  // <- import UrlMapping
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FrontControllerServlet extends HttpServlet {

    private static Set<String> uris = new HashSet<>();
    private static List<String> controllerClasses = new ArrayList<>();
    private static Map<String, MethodInfo> urlMappings = new HashMap<>();
    private static Map<String, List<MethodInfo>> allMethodsByClass = new HashMap<>();

    @Override
    public void init() throws ServletException {
        System.out.println("[Framework] Scan des classes...");

        String packageToScan = getServletContext().getInitParameter("controllerPackage");
        if (packageToScan == null || packageToScan.isEmpty()) {
            packageToScan = "controlleur";
        }

        List<String> allClasses = PackageScanner.getClasses(packageToScan);

        for (String className : allClasses) {
            try {
                Class<?> clazz = Class.forName(className);

                if (clazz.isAnnotationPresent(Controller.class)) {
                    controllerClasses.add(className);
                    System.out.println("[Framework] Controller: " + className);

                    List<MethodInfo> methods = new ArrayList<>();

                    for (Method method : clazz.getMethods()) {
                        if (method.isAnnotationPresent(UrlMapping.class)) {
                            UrlMapping rm = method.getAnnotation(UrlMapping.class);
                            String url = rm.value();
                            MethodInfo info = new MethodInfo(className, method.getName(), url);
                            urlMappings.put(url, info);
                            methods.add(info);
                            System.out.println(
                                    "[Framework] Mapping: " + url + " -> " + className + "." + method.getName());
                        }
                    }

                    if (!methods.isEmpty()) {
                        allMethodsByClass.put(className, methods);
                    }
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        System.out.println("[Framework] Total controllers: " + controllerClasses.size());
        System.out.println("[Framework] Total mappings: " + urlMappings.size());
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

    protected void processRequest(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String uri = req.getRequestURI();
        String contextPath = req.getContextPath();
        String chemin = uri.substring(contextPath.length());

        uris.add(chemin);

        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();

        out.println("<!DOCTYPE html>");
        out.println("<html><head><title>Framework</title></head><body>");
        out.println("<h1>URL: " + chemin + "</h1>");

        if (urlMappings.containsKey(chemin)) {
            MethodInfo info = urlMappings.get(chemin);
            out.println("<h2>Mapping trouve</h2>");
            out.println("<p>Classe: " + info.className + "</p>");
            out.println("<p>Methode: " + info.methodName + "</p>");
        } else {
            out.println("<h2>Erreur 404 - URL non trouvee</h2>");
            out.println("<p>Aucun mapping pour l'URL: " + chemin + "</p>");

            out.println("<h2>Liste des controllers et leurs mappings</h2>");

            if (allMethodsByClass.isEmpty()) {
                out.println("<p>Aucun controller trouve</p>");
            } else {
                for (Map.Entry<String, List<MethodInfo>> entry : allMethodsByClass.entrySet()) {
                    out.println("<h3>Classe: " + entry.getKey() + "</h3>");
                    out.println("<ul>");
                    for (MethodInfo m : entry.getValue()) {
                        out.println("<li>" + m.url + " -> " + m.methodName + "</li>");
                    }
                    out.println("</ul>");
                }
            }
        }

        out.println("<h2>URLs capturees (" + uris.size() + ")</h2><ul>");
        for (String lien : uris) {
            out.println("<li>" + lien + "</li>");
        }
        out.println("</ul>");

        out.println("</body></html>");
    }

    private static class MethodInfo {
        String className;
        String methodName;
        String url;

        MethodInfo(String className, String methodName, String url) {
            this.className = className;
            this.methodName = methodName;
            this.url = url;
        }
    }
}