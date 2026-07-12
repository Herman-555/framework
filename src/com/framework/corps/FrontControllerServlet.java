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

public class FrontControllerServlet extends HttpServlet {
    List<String> controllers = new ArrayList<>();
    Map<String, Method> mappings = new HashMap<>();
    Map<String, Object> controllerss = new HashMap<>();

    @Override
    public void init() {
        try {
            Enumeration<java.net.URL> roots = Thread.currentThread().getContextClassLoader().getResources("controllerpackage");
            while (roots.hasMoreElements()) {
                File dir = new File(URLDecoder.decode(roots.nextElement().getFile(), StandardCharsets.UTF_8));
                scanClasses(dir, "controllerpackage");
            }
            int i = 0;
        } catch (Exception e) {}
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
                        controllerss.put(clazz.getSimpleName(), instance);
                        for (Method m : clazz.getDeclaredMethods()) {
                            if (m.isAnnotationPresent(com.framework.Mapping.UrlMapping.class)) {
                                mappings.put(m.getAnnotation(com.framework.Mapping.UrlMapping.class).value(), m);
                            }
                        }
                    }
                } catch (Exception e) {}
            }
        }
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
        if (mappings.containsKey(path)) {
            Method method = mappings.get(path);
            out.println("<p>URL trouve , Methode : </p> " + mappings.get(path).getName());
        } else {
            out.println("<p>URLs disponibles:</p><ul>");
            for(String controllerr : controllers){
                out.println("<p>Controller : " + controllerr + "</p>");
            }
            for (String urls : mappings.keySet()) {
                out.println("<li><strong>URL :</strong> " + urls + " - <strong>Méthode :</strong> " + mappings.get(urls).getName() + "</li>");
            }
            out.println("</ul>");
        }
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        doGet(req, resp);
    }
}