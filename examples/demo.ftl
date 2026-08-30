<#-- FreeMarker Plus demo -->
<#assign title = "Hello ${user.name}">

<!DOCTYPE html>
<html>
<head>
  <title>${title}</title>
  <style>
    .card { border: 1px solid #ccc; color: green; }
    /* a css comment */
  </style>
  <script>
    var count = 42; // a js comment
    function greet(name) { return "Hi " + name; }
  </script>
</head>
<body>
  <#if user.age gt 18>
    <div class="card" id="main">Welcome, ${user.name}!</div>
  <#else>
    <div class="card">Sorry, too young.</div>
  </#if>
  <#list items as item>
    <p>${item}</p>
  </#list>
</body>
</html>
