<b>${data.title?html}</b>

Marked: ${data.selected?c} of ${data.total?c}. Tap an ingredient to mark or unmark it.<#if (data.pages > 1)> Page ${data.page?c} of ${data.pages?c}.</#if><#if data.empty>

Nothing found.</#if>
