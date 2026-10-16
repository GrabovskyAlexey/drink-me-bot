package ru.grabovsky.drinkmebot.strategy.flow.shopping

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.grabovsky.drinkmebot.model.ShoppingItem
import ru.grabovsky.drinkmebot.service.interfaces.DataLocaleService
import ru.grabovsky.drinkmebot.service.interfaces.I18nService
import ru.grabovsky.drinkmebot.service.interfaces.ShoppingListService
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.AnswerCallbackAction
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.DeleteMessageAction
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.EditMessageAction
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowCallbackPayload
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowContext
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowHandler
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowInlineButton
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowKey
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowKeys
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowMessage
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowParseMode
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowResult
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowStartContext
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowStep
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.SendMessageAction
import ru.grabovsky.drinkmebot.strategy.flow.core.support.PAGE_NOOP
import ru.grabovsky.drinkmebot.strategy.flow.core.support.buildMessage
import ru.grabovsky.drinkmebot.strategy.flow.core.support.paginationButtons
import java.util.Locale

enum class ShoppingStep(override val key: String) : FlowStep {
    MAIN("main"),
}

/** Состояния нет: список читается из БД при каждой перерисовке. */
data class ShoppingState(val unused: Boolean = false)

data class ShoppingMainModel(val total: Int, val bought: Int, val page: Int, val pages: Int)

/**
 * `/shopping`: список покупок. Каждая позиция - кнопка, нажатие отмечает её купленной (✅) и обратно.
 * Callback-кнопки: `T:<id товара>:<страница>`, `P:<страница>`, `D` (купленное - в «Мои ингредиенты»), `CLR` (очистить), `CLOSE`.
 */
@Component
class ShoppingFlow(
    private val shopping: ShoppingListService,
    private val i18n: I18nService,
    private val dataLocale: DataLocaleService,
) : FlowHandler<ShoppingState> {

    override val key: FlowKey = FlowKeys.SHOPPING
    override val payloadType: Class<ShoppingState> = ShoppingState::class.java

    override fun start(context: FlowStartContext): FlowResult<ShoppingState> =
        FlowResult(ShoppingStep.MAIN.key, ShoppingState(), listOf(SendMessageAction(MAIN_BINDING, message(context.user.id, context.locale, 0))))

    override fun onMessage(context: FlowContext<ShoppingState>, message: Message): FlowResult<ShoppingState>? = null

    override fun onCallback(context: FlowContext<ShoppingState>, callbackQuery: CallbackQuery, data: String): FlowResult<ShoppingState>? {
        val userId = context.user.id
        val locale = context.locale
        val state = context.state.payload
        val parts = data.split(':')
        var toast: String? = null
        val page = when (parts[0]) {
            "T" -> {
                shopping.toggleBought(userId, parts.getOrNull(1)?.toIntOrNull() ?: return null)
                parts.getOrNull(2)?.toIntOrNull() ?: 0
            }
            "P" -> parts.getOrNull(1)?.toIntOrNull() ?: return null
            "D" -> {
                toast = i18n.i18n("alerts.shopping.moved", locale, null, shopping.moveBoughtToIngredients(userId))
                0
            }
            "CLR" -> {
                shopping.clear(userId)
                0
            }
            PAGE_NOOP -> return FlowResult(context.state.stepKey, state, listOf(AnswerCallbackAction(callbackQuery.id)))
            "CLOSE" -> return FlowResult(
                ShoppingStep.MAIN.key, state, listOf(DeleteMessageAction(MAIN_BINDING), AnswerCallbackAction(callbackQuery.id)), completed = true,
            )
            else -> return null
        }
        return FlowResult(
            ShoppingStep.MAIN.key, state,
            listOf(EditMessageAction(MAIN_BINDING, message(userId, locale, page)), AnswerCallbackAction(callbackQuery.id, toast)),
        )
    }

    private fun message(userId: Long, locale: Locale, requestedPage: Int): FlowMessage {
        val items = shopping.items(userId, dataLocale.resolve(userId, locale))
        val pages = maxOf(1, (items.size + PAGE_SIZE - 1) / PAGE_SIZE)
        val page = requestedPage.coerceIn(0, pages - 1)
        val itemButtons = items.drop(page * PAGE_SIZE).take(PAGE_SIZE).mapIndexed { index, item -> itemButton(item, page, index) }
        val actions = listOfNotNull(
            button(i18n.i18n("buttons.shopping.done", locale), "D", row = PAGE_SIZE + 1).takeIf { items.any { it.bought } },
            button(i18n.i18n("buttons.shopping.clear", locale), "CLR", row = PAGE_SIZE + 2).takeIf { items.isNotEmpty() },
        )
        val buttons = itemButtons +
            key.paginationButtons(i18n, locale, page, pages, row = PAGE_SIZE) { "P:$it" } +
            actions +
            button(i18n.i18n("buttons.common.close", locale), "CLOSE", row = FlowInlineButton.LAST_ROW)
        return key.buildMessage(
            step = ShoppingStep.MAIN, model = ShoppingMainModel(items.size, items.count { it.bought }, page + 1, pages),
            inlineButtons = buttons, parseMode = FlowParseMode.HTML,
        )
    }

    private fun itemButton(item: ShoppingItem, page: Int, row: Int) =
        button("${if (item.bought) "✅" else "⬜"} ${item.name}", "T:${item.goodsId}:$page", row)

    private fun button(text: String, data: String, row: Int) = FlowInlineButton(text, FlowCallbackPayload(key.value, data), row = row)

    companion object {
        const val MAIN_BINDING = "main"
        const val PAGE_SIZE = 10
    }
}
