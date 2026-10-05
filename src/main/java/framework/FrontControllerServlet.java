package framework;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import framework.annotation.Controller;
import framework.annotation.UrlMapping;

public class FrontControllerServlet extends HttpServlet {

    private Map<String, VerbAction> mappings = new HashMap<>();

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            // Récupère le chemin physique du dossier WEB-INF/classes du projet client
            String realPath = getServletContext().getRealPath("/WEB-INF/classes");
            
            if (realPath != null) {
                File classesDir = new File(URLDecoder.decode(realPath, "UTF-8"));
                if (classesDir.exists()) {
                    scanDirectory(classesDir, "");
                }
            }
        } catch (Exception e) {
            throw new ServletException("Erreur lors de l'initialisation du mapping dynamique", e);
        }
    }

    /**
     * Parcours récursif des fichiers .class pour trouver les @Controller et leurs @UrlMapping
     */
    private void scanDirectory(File dir, String packageName) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                String subPackage = packageName.isEmpty() ? file.getName() : packageName + "." + file.getName();
                scanDirectory(file, subPackage);
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + "." + file.getName().replace(".class", "");
                registerClassIfController(className);
            }
        }
    }

    /**
     * Inspecte la classe via Reflection pour enregistrer les routes
     */
    private void registerClassIfController(String className) {
        try {
            Class<?> clazz = Class.forName(className);

            if (clazz.isAnnotationPresent(Controller.class)) {
                Object controllerInstance = clazz.getDeclaredConstructor().newInstance();

                for (Method method : clazz.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(UrlMapping.class)) {
                        String url = method.getAnnotation(UrlMapping.class).value();
                        mappings.put(url, new VerbAction(controllerInstance, method));
                    }
                }
            }
        } catch (Exception e) {
            // Ignore les classes non instanciables ou abstraites
        }
    }

    protected void processRequest(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        
        String uri = req.getRequestURI();
        String contextPath = req.getContextPath();
        String path = uri.substring(contextPath.length());

        // 1. Vérification de l'existence de la route
        VerbAction verbAction = mappings.get(path);
        if (verbAction == null) {
            throw new ServletException("Aucune méthode ou URL associée au chemin : " + path);
        }

        Method method = verbAction.getMethod();
        Object controller = verbAction.getControllerInstance();

        if (method == null || controller == null) {
            throw new ServletException("Action ou instance introuvable pour le chemin : " + path);
        }

        try {
            // 2. Matching des paramètres HTTP avec les arguments de la méthode
            Object[] args = MethodMapper.resolveParameters(method, req);

            // 3. Invocation dynamique
            System.out.println("--> Execution de la méthode : " + method.getName());

            Object result = method.invoke(controller, args);
            
            System.out.println("<-- Fin execution. Résultat retourné : " + result);

            // 4. Redirection vers la vue
            if (result instanceof String) {
                String viewPath = (String) result;
                req.getRequestDispatcher(viewPath).forward(req, resp);
            } else if (result != null) {
                throw new ServletException("Le type de retour de la méthode " + method.getName() + " doit être un String (chemin de la vue)");
            }

        } catch (Exception e) {
            throw new ServletException("Erreur lors de l'exécution de la méthode " + method.getName(), e);
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