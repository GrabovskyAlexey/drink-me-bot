<b>${data.title?html}</b> (${data.total?c})<#if (data.pages > 1)>
Página ${data.page?c} de ${data.pages?c}</#if>
<#if data.filter?has_content>
🔎 Filtro: ${data.filter?html}
</#if>
<#if data.items?has_content>

<#list data.items as item>
${item.number?c}. <b>${item.name?html}</b><#if item.fallback> 🇺🇸</#if><#if item.missing?has_content> — falta: ${item.missing?html}</#if>
</#list>
<#if data.hasFallback>

<i>🇺🇸 — no disponible en tu idioma, se muestra en inglés.</i>
</#if>
<#else>

Aquí todavía no hay nada.
</#if>
