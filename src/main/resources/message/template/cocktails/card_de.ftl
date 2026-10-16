🍸 <b>${data.name?html}</b>
<#if data.fallback>
<i>🇺🇸 In deiner Sprache nicht verfügbar, auf Englisch angezeigt.</i>
</#if>
<#if data.author??>
<i>Autor: ${data.author?html}</i>
</#if>
<#if data.tags?has_content>
${data.tags?html}
</#if>

<b>Zutaten</b>
<#list data.ingredients as line>
${line?html}
</#list>
<#if data.hasMarks>
<i>✅ vorhanden · ❌ fehlt · • im Laden erhältlich</i>
</#if>
<#if data.gadgets?has_content>

<b>Zubehör</b>
${data.gadgets?html}
</#if>
<#if data.steps?has_content>

<b>Zubereitung</b>
<#list data.steps as step>
${step?counter}. ${step?html}
</#list>
</#if>
