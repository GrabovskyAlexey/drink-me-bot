<b>${data.title?html}</b>

Отмечено: ${data.selected?c} из ${data.total?c}. Нажмите на ингредиент, чтобы отметить его или снять отметку.<#if (data.pages > 1)> Страница ${data.page?c} из ${data.pages?c}.</#if><#if data.empty>

Ничего не найдено.</#if>
