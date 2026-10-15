package ru.grabovsky.drinkmebot.strategy.flow.cocktails

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.grabovsky.drinkmebot.model.CocktailCard
import ru.grabovsky.drinkmebot.model.CocktailMatch
import ru.grabovsky.drinkmebot.service.interfaces.CocktailCardService
import ru.grabovsky.drinkmebot.service.interfaces.CocktailCollectionService
import ru.grabovsky.drinkmebot.service.interfaces.CocktailSuggestService
import ru.grabovsky.drinkmebot.service.interfaces.CocktailSuggestService.Companion.MAX_MISSING
import ru.grabovsky.drinkmebot.service.interfaces.DataLocaleService
import ru.grabovsky.drinkmebot.service.interfaces.I18nService
import ru.grabovsky.drinkmebot.service.interfaces.IngredientService
import ru.grabovsky.drinkmebot.service.interfaces.StatsService
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
 * Callback-кнопки: `M`, `S:<раздел>:<страница>`, `C:<id>:<раздел>:<страница>`, `F:`/`K:<id>:<раздел>:<страница>` (избранное, «готовил»), `R`, `B`.
 * Раздел: 0..2 - не хватает N ингредиентов, 10 - безалкогольные, 11 - избранное, -1 - карточка из «Удиви меня».
 */
@Component
class CocktailsFlow(
    private val suggestions: CocktailSuggestService,
    private val cards: CocktailCardService,
    private val collections: CocktailCollectionService,
    private val ingredients: IngredientService,
    private val i18n: I18nService,
    private val dataLocale: DataLocaleService,
    private val stats: StatsService,
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
            "F", "K" -> {
                val cocktailId = parts.intAt(1) ?: return null
                if (parts[0] == "F") collections.toggleFavorite(userId, cocktailId) else collections.toggleCooked(userId, cocktailId)
                cardMessage(userId, locale, cocktailId, parts.intAt(2) ?: 0, parts.intAt(3) ?: 0) ?: return null
            }
            "R" -> randomMessage(userId, locale) ?: (CocktailsStep.MAIN to mainMessage(userId, locale))
            "B" -> CocktailsStep.BUY to buyMessage(userId, locale)
            PAGE_NOOP -> return FlowResult(context.state.stepKey, state, listOf(answer))
            "CLOSE" -> return FlowResult(CocktailsStep.MAIN.key, state, listOf(DeleteMessageAction(MAIN_BINDING), answer), completed = true)
            else -> return null
        }
        return FlowResult(step.key, state, listOf(EditMessageAction(MAIN_BINDING, message), answer))
    }

    private fun mainMessage(userId: Long, locale: Locale): FlowMessage {
        val lang = dataLocale.resolve(userId, locale)
        val counts = suggestions.counts(userId, lang)
        val ready = counts[0] ?: 0
        val missingOne = counts[1] ?: 0
        val missingTwo = counts[2] ?: 0
        val nonAlcoholic = collections.nonAlcoholicCount(lang)
        val favorites = collections.favoritesCount(userId, lang)
        val buttons = listOfNotNull(
            sectionButton(locale, "ready", 0, ready, row = 0),
            sectionButton(locale, "missing1", 1, missingOne, row = 1),
            sectionButton(locale, "missing2", 2, missingTwo, row = 2),
            sectionButton(locale, "nonalcoholic", NON_ALCOHOLIC, nonAlcoholic, row = 3),
            sectionButton(locale, "favorites", FAVORITES, favorites, row = 4),
            FlowInlineButton(i18n.i18n("buttons.cocktails.random", locale), callback("R"), row = 5).takeIf { ready > 0 },
            FlowInlineButton(i18n.i18n("buttons.cocktails.buy", locale), callback("B"), row = 6).takeIf { missingOne > 0 },
            FlowKeys.INGREDIENTS.openButton(i18n.i18n("buttons.cocktails.to_ingredients", locale), row = 7),
            closeButton(locale),
        )
        return simpleMessage(CocktailsStep.MAIN, CocktailsMainModel(ingredients.selectedCount(userId), ready, missingOne, missingTwo), buttons)
    }

    private fun sectionButton(locale: Locale, name: String, section: Int, count: Int, row: Int): FlowInlineButton? =
        FlowInlineButton(i18n.i18n("buttons.cocktails.$name", locale, null, count), callback("S:$section:0"), row = row).takeIf { count > 0 }

    /** Размер раздела: 0..[MAX_MISSING] - подбор по ингредиентам, [NON_ALCOHOLIC] и [FAVORITES] - подборки; иначе `null`. */
    private fun sectionTotal(userId: Long, lang: String, section: Int): Int? = when (section) {
        in 0..MAX_MISSING -> suggestions.counts(userId, lang)[section] ?: 0
        NON_ALCOHOLIC -> collections.nonAlcoholicCount(lang)
        FAVORITES -> collections.favoritesCount(userId, lang)
        else -> null
    }

    private fun sectionPage(userId: Long, lang: String, section: Int, page: Int): List<CocktailMatch> = when (section) {
        NON_ALCOHOLIC -> collections.nonAlcoholic(lang, page, PAGE_SIZE)
        FAVORITES -> collections.favorites(userId, lang, page, PAGE_SIZE)
        else -> suggestions.findCocktails(userId, lang, section, page, PAGE_SIZE)
    }

    private fun listMessage(userId: Long, locale: Locale, section: Int, requestedPage: Int): FlowMessage? {
        val lang = dataLocale.resolve(userId, locale)
        val total = sectionTotal(userId, lang, section) ?: return null
        val pages = ((total + PAGE_SIZE - 1) / PAGE_SIZE).coerceAtLeast(1)
        val page = requestedPage.coerceIn(0, pages - 1)
        val found = sectionPage(userId, lang, section, page)

        val items = found.mapIndexed { index, match ->
            CocktailsListItem(page * PAGE_SIZE + index + 1, match.name, match.missingGoods.joinToString(", ") { it.name })
        }
        val buttons = found.mapIndexed { index, match ->
            FlowInlineButton(i18n.i18n("buttons.cocktails.cocktail", locale, null, match.name), callback("C:${match.cocktailId}:$section:$page"), row = index)
        } + key.paginationButtons(i18n, locale, page, pages, PAGE_SIZE) { "S:$section:$it" } + listOf(backButton(locale))
        val title = i18n.i18n("cocktails.section.${SECTIONS.getValue(section)}", locale)
        return simpleMessage(CocktailsStep.LIST, CocktailsListModel(title, total, page + 1, pages, items), buttons)
    }

    /** Случайный коктейль из раздела «можно приготовить»; `null`, если ничего не готово. */
    private fun randomMessage(userId: Long, locale: Locale): Pair<CocktailsStep, FlowMessage>? {
        val match = collections.randomReady(userId, dataLocale.resolve(userId, locale)) ?: return null
        return cardMessage(userId, locale, match.cocktailId, RANDOM, 0)
    }

    /**
     * Карточка коктейля; кнопка «Назад» возвращает в тот же раздел на ту же страницу ([RANDOM] - на главный экран, там же
     * кнопка «ещё один случайный»). Нет такого коктейля - возвращаем к разделу.
     */
    private fun cardMessage(userId: Long, locale: Locale, cocktailId: Int, section: Int, page: Int): Pair<CocktailsStep, FlowMessage>? {
        val card = cards.card(userId, dataLocale.resolve(userId, locale), cocktailId)
            ?: return listMessage(userId, locale, section, page)?.let { CocktailsStep.LIST to it }
        // Счётчик просмотров вторичен: сбой статистики не должен ломать показ карточки
        runCatching { stats.recordCocktailView(cocktailId) }
        val marks = collections.marks(userId, cocktailId)
        val context = "$cocktailId:$section:$page"
        val random = section == RANDOM
        val buttons = listOfNotNull(
            FlowInlineButton(i18n.i18n("buttons.cocktails.favorite.${if (marks.favorite) "on" else "off"}", locale), callback("F:$context"), row = 0, col = 0),
            FlowInlineButton(i18n.i18n("buttons.cocktails.cooked.${if (marks.cooked) "on" else "off"}", locale), callback("K:$context"), row = 0, col = 1),
            FlowInlineButton(i18n.i18n("buttons.cocktails.random_again", locale), callback("R"), row = 1).takeIf { random },
            if (random) backButton(locale)
            else FlowInlineButton(i18n.i18n("buttons.common.back", locale), callback("S:$section:$page"), row = FlowInlineButton.LAST_ROW),
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
        /** Разделы: 0..2 - подбор по числу недостающих ингредиентов, дальше подборки без подбора. */
        const val NON_ALCOHOLIC = 10
        const val FAVORITES = 11

        /** Признак «карточка открыта кнопкой «Удиви меня»», вместо раздела. */
        const val RANDOM = -1
        private val SECTIONS = mapOf(0 to "ready", 1 to "missing1", 2 to "missing2", NON_ALCOHOLIC to "nonalcoholic", FAVORITES to "favorites")
    }
}
