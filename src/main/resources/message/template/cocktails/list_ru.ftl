<b>${data.title?html}</b> (${data.total?c})<#if (data.pages > 1)>
Страница ${data.page?c} из ${data.pages?c}</#if>
<#if data.filter?has_content>
🔎 Фильтр: ${data.filter?html}
</#if>
<#if data.items?has_content>

<#list data.items as item>
${item.number?c}. <b>${item.name?html}</b><#if item.fallback> 🇺🇸</#if><#if item.missing?has_content> — не хватает: ${item.missing?html}</#if>
</#list>
<#if data.hasFallback>

<i>🇺🇸 — нет перевода на ваш язык, показан английский вариант.</i>
</#if>
<#else>

Здесь пока ничего нет.
</#if>
