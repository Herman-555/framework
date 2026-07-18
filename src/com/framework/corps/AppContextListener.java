package com.framework.corps;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.lang.reflect.Method;

import mg.itu.framework.controller.Controller;
import mg.itu.framework.exception.DuplicateUrlException;
import mg.itu.framework.mapping.Methode;
import mg.itu.framework.mapping.UrlMethod;
    
@WebListener
public class AppContextListener implements ServletContextListener {
    public List<String> controllers = new ArrayList<>();
    public Map<UrlMethod, Methode> mappings = new HashMap<>();
    public Map<String, Object> controllerss = new HashMap<>();

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();

        try {
            Enumeration<java.net.URL> roots = Thread.currentThread().getContextClassLoader().getResources("controllerpackage");
            while (roots.hasMoreElements()) {
                File dir = new File(URLDecoder.decode(roots.nextElement().getFile(), StandardCharsets.UTF_8));
                scanClasses(dir, "controllerpackage");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        }

        context.setAttribute("controllers", controllers);
        context.setAttribute("mappings", mappings);
        context.setAttribute("controllerss", controllerss);
    }

    public void scanClasses(File dir, String pkg) {
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
                                com.framework.Mapping.UrlMapping annotation = m.getAnnotation(com.framework.Mapping.UrlMapping.class);
                                String url = annotation.value();
                                String httpMethod = annotation.method();
                                UrlMethod urlMethod = new UrlMethod(url, httpMethod);
                                if (mappings.containsKey(urlMethod)) {
                                    throw new DuplicateUrlException(
                                        "Duplicate URL mapping: " + url + " with method " + httpMethod
                                    );
                                }
                                mappings.put(urlMethod, new Methode(clazz.getSimpleName(), m.getName()));
                            }
                        }
                    }
                } catch (DuplicateUrlException e) {
                    throw new RuntimeException(e);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }

}
