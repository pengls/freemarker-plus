<#-- FreeMarker Plus navigation demo -->
<#import "lib.ftl" as lib>
<#assign title = "Hello ${lib.greet(user)}">

<#macro card heading>
  <div class="card"><h2>${heading}</h2></div>
</#macro>

<#function greet name>
  <#return "Hi, ${name}">
</#function>

<#list items as item>
  <@card heading=item.name/>
</#list>
