🛒 <b>Shopping list</b>

<#if data.total gt 0>
Items: ${data.total?c}, bought: ${data.bought?c}.

Tap an item to mark it as bought. Bought items can be moved to My ingredients, or you can clear the list.
<#else>
The list is empty. Add what is missing from a cocktail card or from the What to buy page in /cocktails.
</#if>
