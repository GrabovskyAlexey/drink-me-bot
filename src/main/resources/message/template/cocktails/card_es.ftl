<#if data.part != "TAIL">
🍸 <b>${data.name?html}</b>
<#if data.fallback>
<i>🇺🇸 No está disponible en tu idioma, se muestra en inglés.</i>
</#if>
<#if data.author??>
<i>Autor: ${data.author?html}</i>
</#if>
<#if data.tags?has_content>
${data.tags?html}
</#if>

<b>Ingredientes</b>
<#list data.ingredients as line>
${line?html}
</#list>
<#if data.hasMarks>
<i>✅ lo tienes · ❌ falta · • se compra en cualquier tienda</i>
</#if>
</#if>
<#if data.part != "HEAD">
<#if data.gadgets?has_content>

<b>Utensilios</b>
${data.gadgets?html}
</#if>
<#if data.steps?has_content>

<b>Preparación</b>
<#list data.steps as step>
${step?counter}. ${step?html}
</#list>
</#if>
<#if data.part == "TAIL" && !data.gadgets?has_content && !data.steps?has_content>
🍸 <b>${data.name?html}</b>
</#if>
</#if>
