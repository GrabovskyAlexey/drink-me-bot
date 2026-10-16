💡 <b>Was kaufen</b>

<#if data.items?has_content>
Jede dieser Zutaten schaltet neue Cocktails frei: Bei ihnen fehlt nur sie.

<#list data.items as item>
• ${item.name?html} — schaltet Cocktails frei: ${item.count?c}
</#list>
<#else>
Noch kein Vorschlag: Bei keinem Cocktail fehlt nur eine Zutat.
</#if>
