<b>${data.title?html}</b> (${data.total?c})<#if (data.pages > 1)>
Page ${data.page?c} of ${data.pages?c}</#if>
<#if data.items?has_content>

<#list data.items as item>
${item.number?c}. <b>${item.name?html}</b><#if item.missing?has_content> — missing: ${item.missing?html}</#if>
</#list>
<#else>

Nothing here yet.
</#if>
