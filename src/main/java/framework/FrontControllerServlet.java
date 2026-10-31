package framework;

import framework.annotation.Auth;
import framework.annotation.Controller;
import framework.annotation.Role;
import framework.annotation.UrlMapping;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
  private String sessionMapKey;
  private String sessionCookieName;
  private Set<String> configuredRoles;

  @Override
  public void init() throws ServletException {
    super.init();
    ServletContext context = getServletContext();
    sessionMapKey = context.getInitParameter("sessionMapKey");
    sessionCookieName = context.getInitParameter("sessionCookieName");
    configuredRoles = parseRoles(context.getInitParameter("roles"));
    if (sessionMapKey == null || sessionMapKey.trim().isEmpty()) {
      throw new ServletException("Le context-param 'sessionMapKey' est obligatoire.");
    }
    if (sessionCookieName == null || sessionCookieName.trim().isEmpty()) {
      throw new ServletException("Le context-param 'sessionCookieName' est obligatoire.");
    }
    if (context.getAttribute(sessionMapKey) == null) {
      context.setAttribute(sessionMapKey, new ConcurrentHashMap<String, MySession>());
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

  private Set<String> parseRoles(String rolesParameter) {
    Set<String> roles = new HashSet<String>();
    if (rolesParameter == null) return roles;
    for (String role : rolesParameter.split(",")) {
      if (!role.trim().isEmpty()) roles.add(role.trim());
    }
    return roles;
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
        Object controllerInstance = Constructeur.construireObjet(clazz);
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
    Map<String, MySession> sessionsMap = (Map<String, MySession>) context.getAttribute(sessionMapKey);

    String sessionUuid = null;
    Cookie[] cookies = req.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (sessionCookieName.equals(cookie.getName())) {
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

    boolean authenticated = clientSession != null && clientSession.isAuthenticated();
    if (method.isAnnotationPresent(Auth.class) && !authenticated) {
      req.getRequestDispatcher("/error.jsp").forward(req, resp);
      return;
    }
    Role roleAnnotation = method.getAnnotation(Role.class);
    if (roleAnnotation != null && (!authenticated || !hasAllowedRole(clientSession, roleAnnotation))) {
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
          Cookie sessionCookie = new Cookie(sessionCookieName, sessionUuid);
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

  private boolean hasAllowedRole(MySession session, Role roleAnnotation) {
    for (String role : roleAnnotation.value()) {
      if (role == null || !configuredRoles.contains(role.trim())) continue;
      if (session.hasRole(role.trim())) return true;
    }
    return false;
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
