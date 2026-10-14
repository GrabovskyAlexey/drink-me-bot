package ru.grabovsky.drinkmebot.strategy.flow.ingredients

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.grabovsky.drinkmebot.model.IngredientCategory
import ru.grabovsky.drinkmebot.model.IngredientItem
import ru.grabovsky.drinkmebot.service.interfaces.DataLocaleService
import ru.grabovsky.drinkmebot.service.interfaces.I18nService
import ru.grabovsky.drinkmebot.service.interfaces.IngredientService
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.AnswerCallbackAction
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.BindMessageAction
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.DeleteMessageAction
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.DeleteMessageIdAction
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.EditMessageAction
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowAction
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
import ru.grabovsky.drinkmebot.strategy.flow.core.support.OPEN
import ru.grabovsky.drinkmebot.strategy.flow.core.support.PAGE_NOOP
import ru.grabovsky.drinkmebot.strategy.flow.core.support.buildMessage
import ru.grabovsky.drinkmebot.strategy.flow.core.support.openButton
import ru.grabovsky.drinkmebot.strategy.flow.core.support.paginationButtons
import java.util.Locale

enum class IngredientsStep(override val key: String) : FlowStep {
    MAIN("main"),
    LIST("list"),
    SEARCH_WAIT("search_wait"),
    RESET_CONFIRM("reset_confirm"),
}

/** [query] - последний поисковый запрос (список «S»); категории состояния не требуют: вид передаётся в callback. */
data class IngredientsState(val query: String? = null)

data class IngredientsMainModel(val selected: Int)

data class IngredientsListModel(val title: String, val selected: Int, val total: Int, val page: Int, val pages: Int, val empty: Boolean)

data class IngredientsResetModel(val selected: Int)

/**
 * `/ingredients`: пользователь отмечает, какие обязательные ингредиенты (алкоголь, ликёры, вермуты, биттеры) у него есть.
 * Всё происходит в одном сообщении, которое перерисовывается: категории -> страница ингредиентов с ✅ -> поиск.
 * Callback-кнопки несут вид списка (`V:<категория|S>:<страница>`, `T:<id>:<вид>:<страница>`), поэтому работают и без состояния.
 */
@Component
class IngredientsFlow(
    private val ingredients: IngredientService,
    private val i18n: I18nService,
    private val dataLocale: DataLocaleService,
) : FlowHandler<IngredientsState> {

    override val key: FlowKey = FlowKeys.INGREDIENTS
    override val payloadType: Class<IngredientsState> = IngredientsState::class.java

    /** С аргументом [OPEN] (переход из другого flow) ничего не отправляет: сообщение привяжет `onCallback`. */
    override fun start(context: FlowStartContext): FlowResult<IngredientsState> =
        FlowResult(
            IngredientsStep.MAIN.key, IngredientsState(),
            if (context.args == OPEN) emptyList() else listOf(SendMessageAction(MAIN_BINDING, mainMessage(context.user.id, context.locale))),
        )

    override fun onMessage(context: FlowContext<IngredientsState>, message: Message): FlowResult<IngredientsState>? {
        if (context.state.stepKey != IngredientsStep.SEARCH_WAIT.key) return null
        val query = message.text?.trim()?.takeIf { it.isNotEmpty() }?.take(IngredientService.MAX_QUERY_LENGTH) ?: return null
        val state = IngredientsState(query)
        return FlowResult(
            IngredientsStep.LIST.key, state,
            listOf(
                EditMessageAction(MAIN_BINDING, listMessage(context.user.id, context.locale, SEARCH_VIEW, 0, state)),
                DeleteMessageIdAction(message.messageId),
            ),
        )
    }

    override fun onCallback(
        context: FlowContext<IngredientsState>,
        callbackQuery: CallbackQuery,
        data: String,
    ): FlowResult<IngredientsState>? {
        val userId = context.user.id
        val locale = context.locale
        val state = context.state.payload
        val answer = AnswerCallbackAction(callbackQuery.id)
        val parts = data.split(':')
        return when (parts[0]) {
            OPEN -> {
                val messageId = callbackQuery.message?.messageId ?: return null
                result(
                    IngredientsStep.MAIN, state,
                    BindMessageAction(MAIN_BINDING, messageId),
                    EditMessageAction(MAIN_BINDING, mainMessage(userId, locale)),
                    answer = answer,
                )
            }
            "MAIN", "M" -> result(
                IngredientsStep.MAIN, state,
                EditMessageAction(MAIN_BINDING, mainMessage(userId, locale, parts.getOrNull(1)?.toIntOrNull() ?: 0)),
                answer,
            )
            "V" -> showList(userId, locale, state, parts.getOrNull(1), parts.getOrNull(2)?.toIntOrNull() ?: 0, answer)
            "T" -> toggle(userId, locale, state, parts, callbackQuery)
            "SEARCH" -> result(
                IngredientsStep.SEARCH_WAIT, state,
                EditMessageAction(MAIN_BINDING, simpleMessage(IngredientsStep.SEARCH_WAIT, null, listOf(backButton(locale, mainCallback(0))))),
                answer,
            )
            "RESET" -> result(
                IngredientsStep.RESET_CONFIRM, state,
                EditMessageAction(MAIN_BINDING, resetMessage(userId, locale)),
                answer,
            )
            "RESET_YES" -> {
                ingredients.reset(userId)
                result(IngredientsStep.MAIN, IngredientsState(), EditMessageAction(MAIN_BINDING, mainMessage(userId, locale)), answer)
            }
            "CLOSE" -> FlowResult(IngredientsStep.MAIN.key, state, listOf(DeleteMessageAction(MAIN_BINDING), answer), completed = true)
            PAGE_NOOP -> result(IngredientsStep.valueOf(context.state.stepKey), state, answer = answer)
            else -> null
        }
    }

    private fun toggle(
        userId: Long,
        locale: Locale,
        state: IngredientsState,
        parts: List<String>,
        callbackQuery: CallbackQuery,
    ): FlowResult<IngredientsState>? {
        val goodsId = parts.getOrNull(1)?.toIntOrNull() ?: return null
        val view = parts.getOrNull(2) ?: return null
        val page = parts.getOrNull(3)?.toIntOrNull() ?: 0
        if (ingredients.toggle(userId, goodsId) == null) {
            return result(
                IngredientsStep.LIST, state,
                answer = AnswerCallbackAction(callbackQuery.id, i18n.i18n("alerts.ingredients.unavailable", locale), showAlert = true),
            )
        }
        return showList(userId, locale, state, view, page, AnswerCallbackAction(callbackQuery.id))
    }

    private fun showList(
        userId: Long,
        locale: Locale,
        state: IngredientsState,
        view: String?,
        page: Int,
        answer: AnswerCallbackAction,
    ): FlowResult<IngredientsState>? {
        view ?: return null
        val known = view == SEARCH_VIEW || IngredientCategory.entries.any { it.name == view }
        if (!known) return null
        // Поиск без запроса (состояние потеряно): возвращаемся к категориям
        if (view == SEARCH_VIEW && state.query == null) {
            return result(IngredientsStep.MAIN, state, EditMessageAction(MAIN_BINDING, mainMessage(userId, locale)), answer)
        }
        return result(IngredientsStep.LIST, state, EditMessageAction(MAIN_BINDING, listMessage(userId, locale, view, page, state)), answer)
    }

    private fun result(
        step: IngredientsStep,
        state: IngredientsState,
        vararg actions: FlowAction,
        answer: AnswerCallbackAction? = null,
    ) = FlowResult(step.key, state, actions.toList() + listOfNotNull(answer))

    /** Категории по одной в ряд, [CATEGORY_PAGE_SIZE] на странице: полное название не обрезается. */
    private fun mainMessage(userId: Long, locale: Locale, requestedPage: Int = 0): FlowMessage {
        val categories = ingredients.categories(userId)
        val pages = ((categories.size + CATEGORY_PAGE_SIZE - 1) / CATEGORY_PAGE_SIZE).coerceAtLeast(1)
        val page = requestedPage.coerceIn(0, pages - 1)
        val buttons = categories.drop(page * CATEGORY_PAGE_SIZE).take(CATEGORY_PAGE_SIZE).mapIndexed { index, summary ->
            val label = i18n.i18n(summary.category.messageKey, locale)
            FlowInlineButton(
                i18n.i18n("buttons.ingredients.category", locale, null, label, summary.selected, summary.total),
                callback("V:${summary.category.name}:0"), row = index,
            )
        } + key.paginationButtons(i18n, locale, page, pages, CATEGORY_PAGE_SIZE) { mainCallback(it) } + listOf(
            FlowInlineButton(i18n.i18n("buttons.ingredients.search", locale), callback("SEARCH"), row = ACTIONS_ROW, col = 0),
            FlowInlineButton(i18n.i18n("buttons.ingredients.reset", locale), callback("RESET"), row = ACTIONS_ROW, col = 1),
            FlowKeys.COCKTAILS.openButton(i18n.i18n("buttons.ingredients.to_cocktails", locale), row = COCKTAILS_ROW),
            FlowInlineButton(i18n.i18n("buttons.common.close", locale), callback("CLOSE"), row = FlowInlineButton.LAST_ROW),
        )
        return simpleMessage(IngredientsStep.MAIN, IngredientsMainModel(categories.sumOf { it.selected }), buttons)
    }

    private fun listMessage(userId: Long, locale: Locale, view: String, requestedPage: Int, state: IngredientsState): FlowMessage {
        val lang = dataLocale.resolve(userId, locale)
        val (title, items) = if (view == SEARCH_VIEW) {
            val query = state.query.orEmpty()
            i18n.i18n("ingredients.search_title", locale, null, query) to ingredients.search(userId, lang, query)
        } else {
            val category = IngredientCategory.valueOf(view)
            i18n.i18n(category.messageKey, locale) to ingredients.items(userId, lang, category)
        }
        val pages = ((items.size + PAGE_SIZE - 1) / PAGE_SIZE).coerceAtLeast(1)
        val page = requestedPage.coerceIn(0, pages - 1)
        val pageItems = items.drop(page * PAGE_SIZE).take(PAGE_SIZE)

        val buttons = pageItems.mapIndexed { index, item -> itemButton(locale, item, view, page, index) } +
            key.paginationButtons(i18n, locale, page, pages, PAGE_SIZE) { "V:$view:$it" } +
            backButton(locale, mainCallback(mainPageOf(view)))
        val model = IngredientsListModel(title, items.count { it.selected }, items.size, page + 1, pages, items.isEmpty())
        return simpleMessage(IngredientsStep.LIST, model, buttons)
    }

    private fun itemButton(locale: Locale, item: IngredientItem, view: String, page: Int, row: Int) =
        FlowInlineButton(
            i18n.i18n(if (item.selected) "buttons.common.selected" else "buttons.common.unselected", locale, null, item.name),
            callback("T:${item.id}:$view:$page"), row = row,
        )

    private fun mainCallback(page: Int) = "M:$page"

    /** Страница главного экрана, на которой находится категория (для кнопки «Назад»); поиск возвращает на первую. */
    private fun mainPageOf(view: String): Int =
        IngredientCategory.entries.firstOrNull { it.name == view }?.let { it.ordinal / CATEGORY_PAGE_SIZE } ?: 0

    private fun resetMessage(userId: Long, locale: Locale): FlowMessage =
        simpleMessage(
            IngredientsStep.RESET_CONFIRM,
            IngredientsResetModel(ingredients.selectedCount(userId)),
            listOf(
                FlowInlineButton(i18n.i18n("buttons.ingredients.reset_yes", locale), callback("RESET_YES"), row = 0),
                FlowInlineButton(i18n.i18n("buttons.common.cancel", locale), callback(mainCallback(0)), row = 1),
            ),
        )

    private fun backButton(locale: Locale, target: String) =
        FlowInlineButton(i18n.i18n("buttons.common.back", locale), callback(target), row = FlowInlineButton.LAST_ROW)

    private fun simpleMessage(step: IngredientsStep, model: Any?, buttons: List<FlowInlineButton>) =
        key.buildMessage(step = step, model = model, inlineButtons = buttons, parseMode = FlowParseMode.HTML)

    private fun callback(data: String) = FlowCallbackPayload(key.value, data)

    companion object {
        const val MAIN_BINDING = "main"
        const val SEARCH_VIEW = "S"
        const val PAGE_SIZE = 10
        const val CATEGORY_PAGE_SIZE = 5
        private const val ACTIONS_ROW = 50
        private const val COCKTAILS_ROW = 51
    }
}
