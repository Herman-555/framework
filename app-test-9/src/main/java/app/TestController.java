package app;

import framework.MySession;
import framework.annotation.Controller;
import framework.annotation.UrlMapping;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Controller
public class TestController {

  private static final String NAMES_ATTRIBUTE = "names";

  @UrlMapping("/home")
  public String home(MySession session) {
    getNames(session);
    return "/index.jsp";
  }

  @UrlMapping("/generate")
  public String generateName(MySession session) {
    getNames(session).add(randomName());
    return "/index.jsp";
  }

  @SuppressWarnings("unchecked")
  private List<String> getNames(MySession session) {
    Object names = session.get(NAMES_ATTRIBUTE);
    if (names instanceof List<?>) {
      return (List<String>) names;
    }

    List<String> newNames = new ArrayList<>();
    session.set(NAMES_ATTRIBUTE, newNames);
    return newNames;
  }

  private String randomName() {
    String[] firstNames = {"Alice", "Bob", "Charlie", "Diane", "Emma", "Lucas"};
    String[] lastNames = {"Martin", "Bernard", "Dubois", "Thomas", "Robert", "Petit"};
    ThreadLocalRandom random = ThreadLocalRandom.current();
    return firstNames[random.nextInt(firstNames.length)] + " "
      + lastNames[random.nextInt(lastNames.length)];
  }
}
