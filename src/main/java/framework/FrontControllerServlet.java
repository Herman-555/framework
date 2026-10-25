package framework;

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
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@MultipartConfig(
  fileSizeThreshold = 1024 * 1024 * 2, // 2MB
  maxFileSize = 1024 * 1024 * 10, // 10MB
  maxRequestSize = 1024 * 1024 * 50 // 50MB
)
public class FrontControllerServlet extends HttpServlet {

  private Map<String, VerbAction> mappings = new HashMap<>();
  private static final String SESSION_MAP_KEY = "appSessionsMap";
  private static final String SESSION_COOKIE_NAME = "app-test";

  @Override
  public void init() throws ServletException {
    super.init();
    ServletContext context = getServletContext();

    // Initialisation de la Map globale des sessions dans le ServletContext si elle n'existe pas
    if (context.getAttribute(SESSION_MAP_KEY) == null) {
      context.setAttribute(
        SESSION_MAP_KEY,
        new ConcurrentHashMap<String, MySession>()
      );
    }

    try {
      String realPath = context.getRealPath("/WEB-INF/classes");
      if (realPath != null) {
        File classesDir = new File(URLDecoder.decode(realPath, "UTF-8"));
        if (classesDir.exists()) {
          scanDirectory(classesDir, "");
        }
      }
    } catch (Exception e) {
      throw new ServletException(
        "Erreur lors de l'initialisation du mapping dynamique",
        e
      );
    }
  }

  private void scanDirectory(File dir, String packageName) {
    File[] files = dir.listFiles();
    if (files == null) return;

    for (File file : files) {
      if (file.isDirectory()) {
        String subPackage = packageName.isEmpty()
          ? file.getName()
          : packageName + "." + file.getName();
        scanDirectory(file, subPackage);
      } else if (file.getName().endsWith(".class")) {
        String className =
          packageName + "." + file.getName().replace(".class", "");
        registerClassIfController(className);
      }
    }
  }

  private void registerClassIfController(String className) {
    try {
      Class<?> clazz = Class.forName(className);

      if (clazz.isAnnotationPresent(Controller.class)) {
        Object controllerInstance = clazz
          .getDeclaredConstructor()
          .newInstance();

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

  protected void processRequest(
    HttpServletRequest req,
    HttpServletResponse resp
  ) throws ServletException, IOException {
    // --- Gestion de la session multi-client (app-test) ---
    ServletContext context = getServletContext();
    @SuppressWarnings("unchecked")
    Map<String, MySession> sessionsMap = (Map<
      String,
      MySession
    >) context.getAttribute(SESSION_MAP_KEY);

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

    MySession clientSession = null;
    if (sessionUuid != null) {
      clientSession = sessionsMap.get(sessionUuid);
    }

    // Si la session n'existe pas encore ou n'est plus valide, on la crée
    if (clientSession == null) {
      sessionUuid = UUID.randomUUID().toString();
      clientSession = new MySession();
      sessionsMap.put(sessionUuid, clientSession);

      Cookie sessionCookie = new Cookie(SESSION_COOKIE_NAME, sessionUuid);
      sessionCookie.setPath(
        req.getContextPath().isEmpty() ? "/" : req.getContextPath()
      );
      sessionCookie.setMaxAge(60 * 60 * 24); // Ex: 1 jour
      resp.addCookie(sessionCookie);
    }

    // Rendre la session accessible via un attribut de requête si besoin
    req.setAttribute("mySession", clientSession);

    // --- Routage et exécution ---
    String uri = req.getRequestURI();
    String contextPath = req.getContextPath();
    String path = uri.substring(contextPath.length());

    VerbAction verbAction = mappings.get(path);
    if (verbAction == null) {
      throw new ServletException(
        "Aucune méthode ou URL associée au chemin : " + path
      );
    }

    Method method = verbAction.getMethod();
    Object controller = verbAction.getControllerInstance();

    if (method == null || controller == null) {
      throw new ServletException(
        "Action ou instance introuvable pour le chemin : " + path
      );
    }

    try {
      Object result;
      Class<?>[] parameterTypes = method.getParameterTypes();

      // Injection automatique de MySession si demandé en paramètre
      if (
        parameterTypes.length == 1 &&
        (parameterTypes[0].getName().equals(MySession.class.getName()) ||
          parameterTypes[0].getSimpleName().equals("MySession"))
      ) {
        System.out.println(
          "--> Exécution de la méthode avec injection de MySession : " +
            method.getName()
        );
        result = method.invoke(controller, clientSession);
      }
      // Sinon, exécution sans paramètre
      else if (parameterTypes.length == 0) {
        System.out.println(
          "--> Exécution de la méthode sans paramètre : " + method.getName()
        );
        result = method.invoke(controller);
      } else {
        throw new ServletException(
          "La méthode " +
            method.getName() +
            " possède des paramètres non pris en charge (" +
            parameterTypes.length +
            " paramètre(s) détecté(s))."
        );
      }

      System.out.println("<-- Fin exécution. Résultat retourné : " + result);

      if (result instanceof String) {
        String viewPath = (String) result;
        req.getRequestDispatcher(viewPath).forward(req, resp);
      } else if (result != null) {
        throw new ServletException(
          "Le type de retour de la méthode " +
            method.getName() +
            " doit être un String (chemin de la vue)"
        );
      }
    } catch (Exception e) {
      throw new ServletException(
        "Erreur lors de l'exécution de la méthode " + method.getName(),
        e
      );
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
