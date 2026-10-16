🛒 <b>Lista della spesa</b>

<#if data.total gt 0>
Voci: ${data.total?c}, acquistate: ${data.bought?c}.

Tocca una voce per segnarla come acquistata. Ciò che hai acquistato si può spostare in «I miei ingredienti», oppure puoi svuotare la lista.
<#else>
La lista è vuota. Aggiungi ciò che manca dalla scheda di un cocktail o dalla pagina «Cosa comprare» in /cocktails.
</#if>
