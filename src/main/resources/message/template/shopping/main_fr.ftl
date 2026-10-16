🛒 <b>Liste de courses</b>

<#if data.total gt 0>
Éléments : ${data.total?c}, achetés : ${data.bought?c}.

Touchez un élément pour le marquer comme acheté. Les achats peuvent passer dans « Mes ingrédients », ou vous pouvez vider la liste.
<#else>
La liste est vide. Ajoutez ce qui manque depuis la fiche d’un cocktail ou depuis « Quoi acheter » dans /cocktails.
</#if>
