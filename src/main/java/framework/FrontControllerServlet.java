package framework;

import framework.annotation.Auth;
import framework.annotation.Controller;
import framework.annotation.UrlMapping;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class FrontControllerServlet extends HttpServlet {
  private final Map<String, VerbAction> mappings = new HashMap<String, VerbAction>();
  private static final String SESSION_MAP_KEY = "appSessionsMap";
  private static final String SESSION_COOKIE_NAME = "app-test";

  @Override
  public void init() throws ServletException {
    super.init();
    ServletContext context = getServletContext();
    if (context.getAttribute(SESSION_MAP_KEY) == null) {
      context.setAttribute(SESSION_MAP_KEY, new ConcurrentHashMap<String, MySession>());
    }
    try {
      String realPath = context.getRealPath("/WEB-INF/classes");
      if (realPath != null) {
        File classesDir = new File(URLDecoder.decode(realPath, "UTF-8"));
        if (classesDir.exists()) scanDirectory(classesDir, "");
      }
    } catch (Exception e) {
      throw new ServletException("Erreur lors de l'initialisation du mapping dynamique", e);
    }
  }

  private void scanDirectory(File dir, String packageName) {
    File[] files = dir.listFiles();
    if (files == null) return;
    for (File file : files) {
      if (file.isDirectory()) {
        String subPackage = packageName.length() == 0 ? file.getName() : packageName + "." + file.getName();
        scanDirectory(file, subPackage);
      } else if (file.getName().endsWith(".class")) {
        String className = packageName + "." + file.getName().replace(".class", "");
        registerClassIfController(className);
      }
    }
  }

  private void registerClassIfController(String className) {
    try {
      Class<?> clazz = Class.forName(className);
      if (clazz.isAnnotationPresent(Controller.class)) {
        Object controllerInstance = clazz.getDeclaredConstructor().newInstance();
        for (Method method : clazz.getDeclaredMethods()) {
          if (method.isAnnotationPresent(UrlMapping.class)) {
            mappings.put(method.getAnnotation(UrlMapping.class).value(), new VerbAction(controllerInstance, method));
          }
        }
      }
    } catch (Exception ignored) {
      // Ignore les classes qui ne sont pas des contrôleurs instanciables.
    }
  }

  protected void processRequest(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    ServletContext context = getServletContext();
    @SuppressWarnings("unchecked")
    Map<String, MySession> sessionsMap = (Map<String, MySession>) context.getAttribute(SESSION_MAP_KEY);

    String sessionUuid = null;
    Cookie[] cookies = req.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (SESSION_COOKIE_NAME.equals(cookie.getName())) {
          sessionUuid = cookie.getValue();
          break;
        }
      }
    }
    MySession clientSession = sessionUuid == null ? null : sessionsMap.get(sessionUuid);

    String uri = req.getRequestURI();
    String path = uri.substring(req.getContextPath().length());
    VerbAction action = mappings.get(path);
    if (action == null) throw new ServletException("Aucune méthode ou URL associée au chemin : " + path);
    Method method = action.getMethod();
    Object controller = action.getControllerInstance();
    if (method == null || controller == null) throw new ServletException("Action ou instance introuvable pour le chemin : " + path);

    if (method.isAnnotationPresent(Auth.class)
        && (clientSession == null || !Boolean.TRUE.equals(clientSession.get("authenticated")))) {
      req.getRequestDispatcher("/error.jsp").forward(req, resp);
      return;
    }

    try {
      Object result;
      Class<?>[] parameterTypes = method.getParameterTypes();
      if (parameterTypes.length == 1 && parameterTypes[0].equals(MySession.class)) {
        if (clientSession == null) {
          clientSession = new MySession();
          sessionUuid = UUID.randomUUID().toString();
          sessionsMap.put(sessionUuid, clientSession);
          Cookie sessionCookie = new Cookie(SESSION_COOKIE_NAME, sessionUuid);
          sessionCookie.setPath(req.getContextPath().length() == 0 ? "/" : req.getContextPath());
          sessionCookie.setMaxAge(60 * 60 * 24);
          resp.addCookie(sessionCookie);
        }
        req.setAttribute("mySession", clientSession);
        result = method.invoke(controller, clientSession);
      } else if (parameterTypes.length == 0) {
        result = method.invoke(controller);
      } else {
        throw new ServletException("La méthode " + method.getName() + " possède des paramètres non pris en charge.");
      }
      if (result instanceof String) req.getRequestDispatcher((String) result).forward(req, resp);
      else if (result != null) throw new ServletException("Le type de retour de la méthode doit être un String (chemin de la vue).");
    } catch (Exception e) {
      throw new ServletException("Erreur lors de l'exécution de la méthode " + method.getName(), e);
    }
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    processRequest(req, resp);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    processRequest(req, resp);
  }
}
