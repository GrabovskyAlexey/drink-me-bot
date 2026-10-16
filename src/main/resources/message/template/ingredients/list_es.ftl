<b>${data.title?html}</b>

Marcados: ${data.selected?c} de ${data.total?c}. Toca un ingrediente para marcarlo o desmarcarlo.<#if (data.pages > 1)> Página ${data.page?c} de ${data.pages?c}.</#if><#if data.empty>

No se encontró nada.</#if>
