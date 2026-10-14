💡 <b>What to buy</b>

<#if data.items?has_content>
Each of these ingredients unlocks new cocktails: they are missing only this one.

<#list data.items as item>
• ${item.name?html} — unlocks cocktails: ${item.count?c}
</#list>
<#else>
Nothing to suggest yet: no cocktail is missing just one ingredient.
</#if>
