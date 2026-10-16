💡 <b>Quoi acheter</b>

<#if data.items?has_content>
Chacun de ces ingrédients débloque de nouveaux cocktails : il ne leur manque que lui.

<#list data.items as item>
• ${item.name?html} — débloque des cocktails : ${item.count?c}
</#list>
<#else>
Rien à conseiller pour l'instant : aucun cocktail ne manque d'un seul ingrédient.
</#if>
