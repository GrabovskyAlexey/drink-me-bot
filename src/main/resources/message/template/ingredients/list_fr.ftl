<b>${data.title?html}</b>

Cochés : ${data.selected?c} sur ${data.total?c}. Touchez un ingrédient pour le cocher ou le décocher.<#if (data.pages > 1)> Page ${data.page?c} sur ${data.pages?c}.</#if><#if data.empty>

Rien trouvé.</#if>
