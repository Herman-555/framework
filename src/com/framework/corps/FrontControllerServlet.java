package com.framework.corps;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mg.itu.framework.controller.Controller;
import mg.itu.framework.exception.DuplicateUrlException;
import mg.itu.framework.mapping.Methode;
import mg.itu.framework.mapping.UrlMethod;

public class FrontControllerServlet extends HttpServlet {
    List<String> controllers = new ArrayList<>();
    Map<UrlMethod, Methode> mappings = new HashMap<>();
    Map<String, Object> controllerss = new HashMap<>();

    @Override
    public void init() {
        controllers = (List<String>) getServletContext().getAttribute("controllers");
        mappings = (Map<UrlMethod, Methode>) getServletContext().getAttribute("mappings");
        controllerss = (Map<String, Object>) getServletContext().getAttribute("controllerss");
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String uri = req.getRequestURI();
        String url = req.getRequestURL().toString();
        String chemin = req.getServletPath();
        String contexte = req.getContextPath();
        String parametres = req.getQueryString();

        resp.setContentType("text/html;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        out.println("<!DOCTYPE html><html><head><title>Front Controller</title></head><body>");
        out.println("<h1>URL capturee par le Front Controller !</h1><hr/>");
        out.println("<h2>Informations sur l'URL :</h2><ul>");
        out.println("<li><strong>URI :</strong> " + uri + "</li>");
        out.println("<li><strong>URL complete :</strong> " + url + "</li>");
        out.println("<li><strong>Chemin :</strong> " + chemin + "</li>");
        out.println("<li><strong>Contexte :</strong> " + contexte + "</li>");
        out.println("<li><strong>Paramètres :</strong> " + (parametres != null ? parametres : "aucune") + "</li>");
        out.println("</ul>");

        out.println("<h1> Liste des controllers</h1><ul>");
        for (String c : controllers) out.println("<li>" + c + "</li>");
        out.println("</ul>");

        System.out.println("URI: " + uri + " | URL: " + url + " | Chemin: " + chemin);

        out.println("<hr/><p>Genere par FrontControllerServlet</p></body></html>");

        String path = uri.substring(contexte.length());
        UrlMethod urlMethod = new UrlMethod(path, req.getMethod());
        
        if (mappings.containsKey(urlMethod)) {
            Methode methode = mappings.get(urlMethod);
            String controllerName = methode.getClassName();
            Object controller = controllerss.get(controllerName);
            out.println("<p>URL trouve, Methode : </p> " + methode.getMethodName());

            try {
                Class<?> clazz = controller.getClass();
                Method method = clazz.getDeclaredMethod(methode.getMethodName());
                method.invoke(controller);
            } catch (Exception e) {
                e.printStackTrace();
                out.println("Erreur: " + e.getMessage());
            }
        } else {
            out.println("<p>URLs disponibles:</p><ul>");
            for (String controllerr : controllers) {
                out.println("<p>Controller : " + controllerr + "</p>");
            }
            for (UrlMethod urls : mappings.keySet()) {
                out.println("<li><strong>URL :</strong> " + urls.getUrl() + " - <strong>Méthode HTTP :</strong> " + urls.getMethod() + " - <strong>Méthode Java :</strong> " + mappings.get(urls).getMethodName() + "</li>");
            }
            out.println("</ul>");
        }
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        doGet(req, resp);
    }
}