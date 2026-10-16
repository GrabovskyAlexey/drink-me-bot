🛒 <b>Lista de la compra</b>

<#if data.total gt 0>
Elementos: ${data.total?c}, comprados: ${data.bought?c}.

Toca un elemento para marcarlo como comprado. Lo comprado se puede pasar a «Mis ingredientes» o puedes vaciar la lista.
<#else>
La lista está vacía. Añade lo que falta desde la ficha de un cóctel o desde «Qué comprar» en /cocktails.
</#if>
