💡 <b>Cosa comprare</b>

<#if data.items?has_content>
Ognuno di questi ingredienti sblocca nuovi cocktail: a loro manca solo quello.

<#list data.items as item>
• ${item.name?html} — sblocca cocktail: ${item.count?c}
</#list>
<#else>
Per ora nessun consiglio: nessun cocktail ha un solo ingrediente mancante.
</#if>
