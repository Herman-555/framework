<!DOCTYPE html>
<html lang="fr">
<head><meta charset="UTF-8"><title>Connexion</title></head>
<body>
  <h1>Connexion</h1>
  <form method="post" action="${pageContext.request.contextPath}/authenticate">
    <button type="submit">S'authentifier</button>
  </form>
  <p><a href="${pageContext.request.contextPath}/page">Page publique</a></p>
  <p><a href="${pageContext.request.contextPath}/members">Page membres (user ou admin)</a></p>
  <p><a href="${pageContext.request.contextPath}/admin">Page admin</a></p>
</body>
</html>
