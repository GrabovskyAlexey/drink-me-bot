package ru.grabovsky.drinkmebot.strategy.flow.cocktails

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.grabovsky.drinkmebot.model.CocktailCard
import ru.grabovsky.drinkmebot.service.interfaces.CocktailCardService
import ru.grabovsky.drinkmebot.service.interfaces.CocktailSuggestService
import ru.grabovsky.drinkmebot.service.interfaces.CocktailSuggestService.Companion.MAX_MISSING
import ru.grabovsky.drinkmebot.service.interfaces.DataLocaleService
import ru.grabovsky.drinkmebot.service.interfaces.I18nService
import ru.grabovsky.drinkmebot.service.interfaces.IngredientService
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.AnswerCallbackAction
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.BindMessageAction
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
import ru.grabovsky.drinkmebot.strategy.flow.core.support.OPEN
import ru.grabovsky.drinkmebot.strategy.flow.core.support.PAGE_NOOP
import ru.grabovsky.drinkmebot.strategy.flow.core.support.buildMessage
import ru.grabovsky.drinkmebot.strategy.flow.core.support.openButton
import ru.grabovsky.drinkmebot.strategy.flow.core.support.paginationButtons
import java.util.Locale

enum class CocktailsStep(override val key: String) : FlowStep {
    MAIN("main"),
    LIST("list"),
    CARD("card"),
    BUY("buy"),
}

/** Состояния нет: все данные приходят в callback, а ингредиенты читаются из БД при каждой перерисовке. */
data class CocktailsState(val unused: Boolean = false)

data class CocktailsMainModel(val selected: Int, val ready: Int, val missingOne: Int, val missingTwo: Int)

data class CocktailsListItem(val number: Int, val name: String, val missing: String)

data class CocktailsListModel(val title: String, val total: Int, val page: Int, val pages: Int, val items: List<CocktailsListItem>)

/** [ingredients] - готовые строки с отметкой («✅ Водка — 50 мл»), [hasMarks] - есть ли отметки «есть / не хватает». */
data class CocktailsCardModel(
    val name: String,
    val author: String?,
    val tags: String,
    val ingredients: List<String>,
    val gadgets: String,
    val steps: List<String>,
    val hasMarks: Boolean,
)

data class CocktailsBuyItem(val name: String, val count: Int)

data class CocktailsBuyModel(val items: List<CocktailsBuyItem>)

/**
 * `/cocktails`: что можно приготовить из отмеченных ингредиентов. Одно сообщение, которое перерисовывается:
 * разделы (можно / не хватает 1 / не хватает 2) -> список коктейлей -> карточка, а также «что купить».
 * Callback-кнопки: `M`, `S:<не хватает>:<страница>`, `C:<id>:<не хватает>:<страница>`, `B`.
 */
@Component
class CocktailsFlow(
    private val suggestions: CocktailSuggestService,
    private val cards: CocktailCardService,
    private val ingredients: IngredientService,
    private val i18n: I18nService,
    private val dataLocale: DataLocaleService,
) : FlowHandler<CocktailsState> {

    override val key: FlowKey = FlowKeys.COCKTAILS
    override val payloadType: Class<CocktailsState> = CocktailsState::class.java

    /** С аргументом [OPEN] (переход из другого flow) ничего не отправляет: сообщение привяжет `onCallback`. */
    override fun start(context: FlowStartContext): FlowResult<CocktailsState> =
        FlowResult(
            CocktailsStep.MAIN.key, CocktailsState(),
            if (context.args == OPEN) emptyList() else listOf(SendMessageAction(MAIN_BINDING, mainMessage(context.user.id, context.locale))),
        )

    override fun onMessage(context: FlowContext<CocktailsState>, message: Message): FlowResult<CocktailsState>? = null

    override fun onCallback(
        context: FlowContext<CocktailsState>,
        callbackQuery: CallbackQuery,
        data: String,
    ): FlowResult<CocktailsState>? {
        val userId = context.user.id
        val locale = context.locale
        val state = context.state.payload
        val answer = AnswerCallbackAction(callbackQuery.id)
        val parts = data.split(':')
        val (step, message) = when (parts[0]) {
            OPEN -> {
                val messageId = callbackQuery.message?.messageId ?: return null
                return FlowResult(
                    CocktailsStep.MAIN.key, state,
                    listOf(BindMessageAction(MAIN_BINDING, messageId), EditMessageAction(MAIN_BINDING, mainMessage(userId, locale)), answer),
                )
            }
            "M" -> CocktailsStep.MAIN to mainMessage(userId, locale)
            "S" -> CocktailsStep.LIST to (listMessage(userId, locale, parts.intAt(1) ?: return null, parts.intAt(2) ?: 0) ?: return null)
            "C" -> cardMessage(userId, locale, parts.intAt(1) ?: return null, parts.intAt(2) ?: 0, parts.intAt(3) ?: 0) ?: return null
            "B" -> CocktailsStep.BUY to buyMessage(userId, locale)
            PAGE_NOOP -> return FlowResult(context.state.stepKey, state, listOf(answer))
            "CLOSE" -> return FlowResult(CocktailsStep.MAIN.key, state, listOf(DeleteMessageAction(MAIN_BINDING), answer), completed = true)
            else -> return null
        }
        return FlowResult(step.key, state, listOf(EditMessageAction(MAIN_BINDING, message), answer))
    }

    private fun mainMessage(userId: Long, locale: Locale): FlowMessage {
        val counts = suggestions.counts(userId, dataLocale.resolve(userId, locale))
        val ready = counts[0] ?: 0
        val missingOne = counts[1] ?: 0
        val missingTwo = counts[2] ?: 0
        val buttons = listOfNotNull(
            sectionButton(locale, "ready", 0, ready, row = 0),
            sectionButton(locale, "missing1", 1, missingOne, row = 1),
            sectionButton(locale, "missing2", 2, missingTwo, row = 2),
            FlowInlineButton(i18n.i18n("buttons.cocktails.buy", locale), callback("B"), row = 3).takeIf { missingOne > 0 },
            FlowKeys.INGREDIENTS.openButton(i18n.i18n("buttons.cocktails.to_ingredients", locale), row = 4),
            closeButton(locale),
        )
        return simpleMessage(CocktailsStep.MAIN, CocktailsMainModel(ingredients.selectedCount(userId), ready, missingOne, missingTwo), buttons)
    }

    private fun sectionButton(locale: Locale, name: String, missing: Int, count: Int, row: Int): FlowInlineButton? =
        FlowInlineButton(i18n.i18n("buttons.cocktails.$name", locale, null, count), callback("S:$missing:0"), row = row).takeIf { count > 0 }

    private fun listMessage(userId: Long, locale: Locale, missing: Int, requestedPage: Int): FlowMessage? {
        if (missing !in 0..MAX_MISSING) return null
        val lang = dataLocale.resolve(userId, locale)
        val total = suggestions.counts(userId, lang)[missing] ?: 0
        val pages = ((total + PAGE_SIZE - 1) / PAGE_SIZE).coerceAtLeast(1)
        val page = requestedPage.coerceIn(0, pages - 1)
        val found = suggestions.findCocktails(userId, lang, missing, page, PAGE_SIZE)

        val items = found.mapIndexed { index, match ->
            CocktailsListItem(page * PAGE_SIZE + index + 1, match.name, match.missingGoods.joinToString(", ") { it.name })
        }
        val buttons = found.mapIndexed { index, match ->
            FlowInlineButton(i18n.i18n("buttons.cocktails.cocktail", locale, null, match.name), callback("C:${match.cocktailId}:$missing:$page"), row = index)
        } + key.paginationButtons(i18n, locale, page, pages, PAGE_SIZE) { "S:$missing:$it" } + listOf(backButton(locale))
        val title = i18n.i18n("cocktails.section.${SECTIONS[missing]}", locale)
        return simpleMessage(CocktailsStep.LIST, CocktailsListModel(title, total, page + 1, pages, items), buttons)
    }

    /** Карточка коктейля; кнопка «Назад» возвращает в тот же раздел на ту же страницу. Нет такого коктейля - возвращаем к разделу. */
    private fun cardMessage(userId: Long, locale: Locale, cocktailId: Int, missing: Int, page: Int): Pair<CocktailsStep, FlowMessage>? {
        val card = cards.card(userId, dataLocale.resolve(userId, locale), cocktailId)
            ?: return listMessage(userId, locale, missing, page)?.let { CocktailsStep.LIST to it }
        val buttons = listOf(
            FlowInlineButton(i18n.i18n("buttons.common.back", locale), callback("S:$missing:$page"), row = FlowInlineButton.LAST_ROW),
        )
        return CocktailsStep.CARD to simpleMessage(CocktailsStep.CARD, cardModel(card), buttons)
    }

    private fun cardModel(card: CocktailCard) = CocktailsCardModel(
        name = card.name,
        author = card.author,
        tags = card.tags.joinToString(" · "),
        ingredients = card.ingredients.map { "${marker(it.essential, it.have)} ${it.name} — ${it.quantity}" },
        gadgets = card.gadgets.joinToString(", "),
        steps = card.steps,
        hasMarks = card.ingredients.any { it.essential },
    )

    private fun marker(essential: Boolean, have: Boolean) = when {
        !essential -> "•"
        have -> "✅"
        else -> "❌"
    }

    private fun buyMessage(userId: Long, locale: Locale): FlowMessage {
        val items = suggestions.whatToBuy(userId, dataLocale.resolve(userId, locale)).map { CocktailsBuyItem(it.goods.name, it.unlocksCount) }
        return simpleMessage(CocktailsStep.BUY, CocktailsBuyModel(items), listOf(backButton(locale)))
    }

    private fun backButton(locale: Locale) =
        FlowInlineButton(i18n.i18n("buttons.common.back", locale), callback("M"), row = FlowInlineButton.LAST_ROW)

    private fun closeButton(locale: Locale) =
        FlowInlineButton(i18n.i18n("buttons.common.close", locale), callback("CLOSE"), row = FlowInlineButton.LAST_ROW)

    private fun simpleMessage(step: CocktailsStep, model: Any?, buttons: List<FlowInlineButton>) =
        key.buildMessage(step = step, model = model, inlineButtons = buttons, parseMode = FlowParseMode.HTML)

    private fun callback(data: String) = FlowCallbackPayload(key.value, data)

    private fun List<String>.intAt(index: Int): Int? = getOrNull(index)?.toIntOrNull()

    companion object {
        const val MAIN_BINDING = "main"
        const val PAGE_SIZE = 8
        private val SECTIONS = listOf("ready", "missing1", "missing2")
    }
}
