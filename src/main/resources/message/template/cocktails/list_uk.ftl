<b>${data.title?html}</b> (${data.total?c})<#if (data.pages > 1)>
Сторінка ${data.page?c} з ${data.pages?c}</#if>
<#if data.filter?has_content>
🔎 Фільтр: ${data.filter?html}
</#if>
<#if data.items?has_content>

<#list data.items as item>
${item.number?c}. <b>${item.name?html}</b><#if item.fallback> 🇺🇸</#if><#if item.missing?has_content> — бракує: ${item.missing?html}</#if>
</#list>
<#if data.hasFallback>

<i>🇺🇸 — немає перекладу вашою мовою, показано англійський варіант.</i>
</#if>
<#else>

Тут поки нічого немає.
</#if>
