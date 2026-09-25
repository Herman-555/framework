package framework;

import framework.annotation.WebApi;
import com.google.gson.Gson;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;

public class FrontControllerServlet extends HttpServlet {

    private Gson gson;

    @Override
    public void init() throws ServletException {
        super.init();

        //Initialisation de json
        this.gson = new Gson();
        
        // C'est ici dans init() qu'on peut scanner les contrôleurs au démarrage 
        // et charger le dictionnaire de routes (Path -> Method)
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
        try {
            // Exemple : Récupération de la méthode invoquée d'après votre routing
            Method targetMethod = getTargetMethodForRequest(req); 
            Object controllerInstance = getControllerInstanceForRequest(req);

            if (targetMethod != null && controllerInstance != null) {
                
                // Exécution de la méthode du contrôleur
                Object result = targetMethod.invoke(controllerInstance);

                // Vérification de la présence de l'annotation @WebApi
                if (targetMethod.isAnnotationPresent(WebApi.class)) {
                    
                    if (result instanceof String) {
                        // Si c'est une chaîne de caractères, on l'affiche directement
                        resp.setContentType("text/plain;charset=UTF-8");
                        PrintWriter out = resp.getWriter();
                        out.print(result);
                        out.flush();
                    } else {
                        // Si ce n'est pas un String, on formate en JSON via le formatter Gson importé
                        resp.setContentType("application/json;charset=UTF-8");
                        String jsonResponse = gson.toJson(result);
                        PrintWriter out = resp.getWriter();
                        out.print(jsonResponse);
                        out.flush();
                    }

                } else {
                    // L'annotation @WebApi n'est pas présente : comportement classique (View/JSP)
                    if (result instanceof String) {
                        String viewPath = (String) result;
                        req.getRequestDispatcher(viewPath).forward(req, resp);
                    } else {
                        // Traitement par défaut si la méthode ne renvoie pas une vue String
                        req.getRequestDispatcher("/index.jsp").forward(req, resp);
                    }
                }
            } else {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Ressource introuvable");
            }

        } catch (Exception e) {
            throw new ServletException("Erreur lors du traitement de la requête", e);
        }
    }

    // Méthodes fictives à adapter selon la logique de votre scanner de routes
    private Method getTargetMethodForRequest(HttpServletRequest req) {
        // Retourne la méthode associée à l'URL demandée
        return null; 
    }

    private Object getControllerInstanceForRequest(HttpServletRequest req) {
        // Retourne l'instance du contrôleur associé
        return null;
    }
}