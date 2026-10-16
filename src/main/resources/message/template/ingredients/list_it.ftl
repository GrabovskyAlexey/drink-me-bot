<b>${data.title?html}</b>

Selezionati: ${data.selected?c} su ${data.total?c}. Tocca un ingrediente per selezionarlo o deselezionarlo.<#if (data.pages > 1)> Pagina ${data.page?c} di ${data.pages?c}.</#if><#if data.empty>

Non è stato trovato niente.</#if>
