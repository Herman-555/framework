<%@ page import="framework.MySession" %>
<%@ page import="java.util.List" %>
<%
  MySession sessionData = (MySession) request.getAttribute("mySession");
  if (sessionData == null) {
    response.sendRedirect(request.getContextPath() + "/home");
    return;
  }
  List<String> names = (List<String>) sessionData.get("names");
%>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <title>Test framework 9</title>
</head>
<body>
    <h1>Noms de cette session</h1>

    <% if (names.isEmpty()) { %>
        <p>Aucun nom n'a encore ete genere.</p>
    <% } else { %>
        <ul>
            <% for (String name : names) { %>
                <li><%= name %></li>
            <% } %>
        </ul>
    <% } %>

    <form method="post" action="<%= request.getContextPath() %>/generate">
        <button type="submit">Generer un nom</button>
    </form>
</body>
</html>
