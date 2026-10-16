<b>${data.title?html}</b> (${data.total?c})<#if (data.pages > 1)>
Pagina ${data.page?c} di ${data.pages?c}</#if>
<#if data.filter?has_content>
🔎 Filtro: ${data.filter?html}
</#if>
<#if data.items?has_content>

<#list data.items as item>
${item.number?c}. <b>${item.name?html}</b><#if item.fallback> 🇺🇸</#if><#if item.missing?has_content> — manca: ${item.missing?html}</#if>
</#list>
<#if data.hasFallback>

<i>🇺🇸 — non disponibile nella tua lingua, mostrato in inglese.</i>
</#if>
<#else>

Qui non c'è ancora niente.
</#if>
