💡 <b>Qué comprar</b>

<#if data.items?has_content>
Cada uno de estos ingredientes desbloquea nuevos cócteles: les falta solo ese.

<#list data.items as item>
• ${item.name?html} — desbloquea cócteles: ${item.count?c}
</#list>
<#else>
Por ahora no hay nada que recomendar: ningún cóctel necesita solo un ingrediente más.
</#if>
