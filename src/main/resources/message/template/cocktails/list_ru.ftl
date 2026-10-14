<b>${data.title?html}</b> (${data.total?c})<#if (data.pages > 1)>
Страница ${data.page?c} из ${data.pages?c}</#if>
<#if data.items?has_content>

<#list data.items as item>
${item.number?c}. <b>${item.name?html}</b><#if item.missing?has_content> — не хватает: ${item.missing?html}</#if>
</#list>
<#else>

Здесь пока ничего нет.
</#if>
