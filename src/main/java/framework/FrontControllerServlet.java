package framework;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;

import framework.annotation.Controller;
import framework.annotation.UrlMapping;
import framework.annotation.WebApi;

public class FrontControllerServlet extends HttpServlet {

    private List<String> controllers = new ArrayList<>();
    private Map<String, VerbAction> mappings = new HashMap<>();
    private Gson gson = new Gson();

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            
            Enumeration<java.net.URL> roots = Thread.currentThread()
                    .getContextClassLoader()
                    .getResources("");

            while (roots.hasMoreElements()) {
                String path = roots.nextElement().getFile();
                File dir = new File(URLDecoder.decode(path, "UTF-8"));
                scanClasses(dir, "");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void scanClasses(File dir, String pkg) {
        if (dir == null || !dir.exists()) return;

        for (File f : dir.listFiles()) {
            if (f.isDirectory()) {
                scanClasses(f, pkg + (pkg.isEmpty() ? "" : ".") + f.getName());
            } else if (f.getName().endsWith(".class")) {
                try {
                    String className = pkg + "." + f.getName().replace(".class", "");
                    Class<?> clazz = Class.forName(className);

                    if (clazz.isAnnotationPresent(Controller.class)) {
                        controllers.add(clazz.getSimpleName());
                        Object instance = clazz.getDeclaredConstructor().newInstance();

                        for (Method m : clazz.getDeclaredMethods()) {
                            if (m.isAnnotationPresent(UrlMapping.class)) {
                                String url = m.getAnnotation(UrlMapping.class).value();
                                mappings.put(url, new VerbAction(instance, m));
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    protected void processRequest(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        String uri = req.getRequestURI();
        String contexte = req.getContextPath();
        String path = uri.substring(contexte.length());

        if (mappings.containsKey(path)) {
            VerbAction verbAction = mappings.get(path);
            Method method = verbAction.getMethod();
            Object controller = verbAction.getControllerInstance();

            try {
                Object result = method.invoke(controller);

                
                if (method.isAnnotationPresent(WebApi.class)) {
                    if (result instanceof String) {
                        
                        resp.setContentType("text/plain;charset=UTF-8");
                        PrintWriter out = resp.getWriter();
                        out.print(result);
                        out.flush();
                    } else {
                        
                        resp.setContentType("application/json;charset=UTF-8");
                        String json = gson.toJson(result);
                        PrintWriter out = resp.getWriter();
                        out.print(json);
                        out.flush();
                    }
                } else {
                    
                    if (result instanceof String) {
                        String viewPath = (String) result;
                        req.getRequestDispatcher(viewPath).forward(req, resp);
                    } else {
                        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                                "Type de retour non valide pour le rendu de vue HTML/JSP");
                    }
                }

            } catch (Exception e) {
                throw new ServletException("Erreur lors de l'exécution de la méthode " + method.getName(), e);
            }
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Aucune route associée à l'URL : " + path);
        }
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