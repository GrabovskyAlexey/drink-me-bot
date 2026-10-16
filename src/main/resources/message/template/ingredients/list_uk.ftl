<b>${data.title?html}</b>

Відзначено: ${data.selected?c} із ${data.total?c}. Натисніть на інгредієнт, щоб відзначити його або зняти позначку.<#if (data.pages > 1)> Сторінка ${data.page?c} з ${data.pages?c}.</#if><#if data.empty>

Нічого не знайдено.</#if>
