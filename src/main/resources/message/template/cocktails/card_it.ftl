🍸 <b>${data.name?html}</b>
<#if data.fallback>
<i>🇺🇸 Non disponibile nella tua lingua, mostrato in inglese.</i>
</#if>
<#if data.author??>
<i>Autore: ${data.author?html}</i>
</#if>
<#if data.tags?has_content>
${data.tags?html}
</#if>

<b>Ingredienti</b>
<#list data.ingredients as line>
${line?html}
</#list>
<#if data.hasMarks>
<i>✅ ce l'hai · ❌ manca · • si compra in negozio</i>
</#if>
<#if data.gadgets?has_content>

<b>Attrezzatura</b>
${data.gadgets?html}
</#if>
<#if data.steps?has_content>

<b>Preparazione</b>
<#list data.steps as step>
${step?counter}. ${step?html}
</#list>
</#if>
