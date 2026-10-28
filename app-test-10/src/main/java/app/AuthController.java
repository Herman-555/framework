package app;

import framework.MySession;
import framework.annotation.Auth;
import framework.annotation.Controller;
import framework.annotation.UrlMapping;

@Controller
public class AuthController {
  @UrlMapping("/authenticate")
  public String authenticate(MySession session) {
    session.set("authenticated", Boolean.TRUE);
    return "/WEB-INF/page-auth.jsp";
  }

  @Auth
  @UrlMapping("/page-auth.jsp")
  public String protectedPage() {
    return "/WEB-INF/page-auth.jsp";
  }

  @UrlMapping("/page")
  public String publicPage() {
    return "/page.jsp";
  }

  @UrlMapping("/logout")
  public String logout(MySession session) {
    session.clear();
    return "/index.jsp";
  }
}
