<!DOCTYPE html>
<html lang="fr">
<head><meta charset="UTF-8"><title>Page authentifiee</title></head>
<body>
  <h1>Vous etes authentifie</h1>
  <form method="post" action="${pageContext.request.contextPath}/logout">
    <button type="submit">Se deconnecter</button>
  </form>
</body>
</html>
