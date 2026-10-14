package ru.grabovsky.drinkmebot.strategy.flow.core.support

import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowCallbackPayload
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowInlineButton
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowKey

/**
 * Callback «открыть главный экран этого flow в текущем сообщении» - для переходов между flow по кнопке.
 * Целевой flow привязывает к себе сообщение, в котором нажали кнопку (`BindMessageAction`), и перерисовывает его;
 * если у flow нет состояния, его `start` с аргументом [OPEN] ничего не отправляет.
 */
const val OPEN = "OPEN"

/** Кнопка перехода на главный экран flow [this] (из другого flow). */
fun FlowKey.openButton(text: String, row: Int, col: Int = 0) =
    FlowInlineButton(text, FlowCallbackPayload(value, OPEN), row = row, col = col)
