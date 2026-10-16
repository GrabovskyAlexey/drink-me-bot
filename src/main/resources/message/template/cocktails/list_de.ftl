<b>${data.title?html}</b> (${data.total?c})<#if (data.pages > 1)>
Seite ${data.page?c} von ${data.pages?c}</#if>
<#if data.filter?has_content>
🔎 Filter: ${data.filter?html}
</#if>
<#if data.items?has_content>

<#list data.items as item>
${item.number?c}. <b>${item.name?html}</b><#if item.fallback> 🇺🇸</#if><#if item.missing?has_content> — fehlt: ${item.missing?html}</#if>
</#list>
<#if data.hasFallback>

<i>🇺🇸 — in deiner Sprache nicht verfügbar, auf Englisch angezeigt.</i>
</#if>
<#else>

Hier ist noch nichts.
</#if>
