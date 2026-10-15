🍸 <b>${data.name?html}</b>
<#if data.fallback>
<i>🇺🇸 Not available in your language, shown in English.</i>
</#if>
<#if data.author??>
<i>Author: ${data.author?html}</i>
</#if>
<#if data.tags?has_content>
${data.tags?html}
</#if>

<b>Ingredients</b>
<#list data.ingredients as line>
${line?html}
</#list>
<#if data.hasMarks>
<i>✅ have · ❌ missing · • can be bought in a shop</i>
</#if>
<#if data.gadgets?has_content>

<b>Equipment</b>
${data.gadgets?html}
</#if>
<#if data.steps?has_content>

<b>Preparation</b>
<#list data.steps as step>
${step?counter}. ${step?html}
</#list>
</#if>
