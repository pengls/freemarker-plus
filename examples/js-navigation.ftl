<#-- FreeMarker Plus JS navigation demo (Phase 2.5) -->
<#assign title = "User dashboard">

<html>
<head>
  <title>${title}</title>
  <script src="app.js"></script>
  <script>
    function login() {
      console.log("login");
    }

    function saveProfile() {
      console.log("saveProfile");
    }
  </script>
</head>
<body>
  <h1>${title}</h1>

  <!-- Ctrl+B on login navigates to function login in the <script> block above -->
  <button onclick="login()">Log in</button>

  <!-- Ctrl+B on saveProfile navigates to the same-file script function -->
  <input type="text" onchange="saveProfile()" placeholder="Profile">

  <!-- Ctrl+B on logout navigates to app.js (external file referenced by <script src>) -->
  <button onclick="logout()">Log out</button>

  <#if title>
    <p>Title is set.</p>
  </#if>
</body>
</html>
