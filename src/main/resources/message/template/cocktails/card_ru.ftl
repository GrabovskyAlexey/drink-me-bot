🍸 <b>${data.name?html}</b>
<#if data.author??>
<i>Автор: ${data.author?html}</i>
</#if>
<#if data.tags?has_content>
${data.tags?html}
</#if>

<b>Ингредиенты</b>
<#list data.ingredients as line>
${line?html}
</#list>
<#if data.hasMarks>
<i>✅ есть · ❌ не хватает · • можно купить в магазине</i>
</#if>
<#if data.gadgets?has_content>

<b>Инвентарь</b>
${data.gadgets?html}
</#if>
<#if data.steps?has_content>

<b>Приготовление</b>
<#list data.steps as step>
${step?counter}. ${step?html}
</#list>
</#if>
