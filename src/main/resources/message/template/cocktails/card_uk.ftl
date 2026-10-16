🍸 <b>${data.name?html}</b>
<#if data.fallback>
<i>🇺🇸 Цього коктейлю вашою мовою немає, показано англійський варіант.</i>
</#if>
<#if data.author??>
<i>Автор: ${data.author?html}</i>
</#if>
<#if data.tags?has_content>
${data.tags?html}
</#if>

<b>Інгредієнти</b>
<#list data.ingredients as line>
${line?html}
</#list>
<#if data.hasMarks>
<i>✅ є · ❌ немає · • можна докупити в магазині</i>
</#if>
<#if data.gadgets?has_content>

<b>Інвентар</b>
${data.gadgets?html}
</#if>
<#if data.steps?has_content>

<b>Приготування</b>
<#list data.steps as step>
${step?counter}. ${step?html}
</#list>
</#if>
