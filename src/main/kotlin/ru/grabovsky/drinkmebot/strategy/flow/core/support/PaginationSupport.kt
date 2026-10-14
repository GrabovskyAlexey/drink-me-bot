package ru.grabovsky.drinkmebot.strategy.flow.core.support

import ru.grabovsky.drinkmebot.service.interfaces.I18nService
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowCallbackPayload
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowInlineButton
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowKey
import java.util.Locale

/** Callback нажатия на номер страницы: ничего не делает, нужен только чтобы кнопка не была пустой. */
const val PAGE_NOOP = "NOOP"

/**
 * Кнопки «назад / страница / вперёд» в ряду [row]; [target] строит данные callback для номера страницы.
 * Для одной страницы кнопок нет; на первой/последней нет «назад»/«вперёд».
 */
fun FlowKey.paginationButtons(
    i18n: I18nService,
    locale: Locale,
    page: Int,
    pages: Int,
    row: Int,
    target: (Int) -> String,
): List<FlowInlineButton> {
    if (pages <= 1) return emptyList()
    fun button(text: String, data: String, col: Int) = FlowInlineButton(text, FlowCallbackPayload(value, data), row = row, col = col)
    return listOfNotNull(
        button(i18n.i18n("buttons.common.prev", locale), target(page - 1), 0).takeIf { page > 0 },
        button(i18n.i18n("buttons.common.page", locale, null, page + 1, pages), PAGE_NOOP, 1),
        button(i18n.i18n("buttons.common.next", locale), target(page + 1), 2).takeIf { page < pages - 1 },
    )
}
