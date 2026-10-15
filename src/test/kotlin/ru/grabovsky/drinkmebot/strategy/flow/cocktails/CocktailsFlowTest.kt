package ru.grabovsky.drinkmebot.strategy.flow.cocktails

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import ru.grabovsky.drinkmebot.model.BuySuggestion
import ru.grabovsky.drinkmebot.model.CocktailFilter
import ru.grabovsky.drinkmebot.model.CocktailTag
import ru.grabovsky.drinkmebot.model.CardIngredient
import ru.grabovsky.drinkmebot.model.CocktailCard
import ru.grabovsky.drinkmebot.model.CocktailMatch
import ru.grabovsky.drinkmebot.model.GoodsRef
import ru.grabovsky.drinkmebot.model.CocktailMarks
import ru.grabovsky.drinkmebot.service.interfaces.CocktailCardService
import ru.grabovsky.drinkmebot.service.interfaces.CocktailCollectionService
import ru.grabovsky.drinkmebot.service.interfaces.CocktailSuggestService
import ru.grabovsky.drinkmebot.service.interfaces.I18nService
import ru.grabovsky.drinkmebot.service.interfaces.IngredientService
import ru.grabovsky.drinkmebot.service.interfaces.StatsService
import org.telegram.telegrambots.meta.api.objects.message.MaybeInaccessibleMessage
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.AnswerCallbackAction
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.BindMessageAction
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.DeleteMessageAction
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.EditMessageAction
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowContext
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowStartContext
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowStateHolder
import ru.grabovsky.drinkmebot.strategy.flow.FakeCocktailFilterService
import ru.grabovsky.drinkmebot.strategy.flow.FakeDataLocaleService
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.SendMessageAction
import java.util.Locale
import org.telegram.telegrambots.meta.api.objects.User as TgUser

class CocktailsFlowTest : ShouldSpec({
    val suggestions = mockk<CocktailSuggestService>()
    val cards = mockk<CocktailCardService>()
    val collections = mockk<CocktailCollectionService>()
    val ingredients = mockk<IngredientService>()
    val i18n = object : I18nService {
        override fun i18n(code: String, locale: Locale, default: String?, vararg args: Any?) =
            if (args.isEmpty()) code else "$code(${args.joinToString(",")})"
    }
    val filters = FakeCocktailFilterService()
    val flow = CocktailsFlow(suggestions, cards, collections, ingredients, i18n, FakeDataLocaleService(), mockk<StatsService>(relaxed = true), filters)
    val ru = Locale.forLanguageTag("ru")
    val tgUser = mockk<TgUser> { every { id } returns 7L }
    val callback = mockk<CallbackQuery> { every { id } returns "cb1" }

    fun context(step: CocktailsStep = CocktailsStep.MAIN) =
        FlowContext(tgUser, ru, FlowStateHolder(step.key, CocktailsState(), mapOf(CocktailsFlow.MAIN_BINDING to 1)))

    fun match(id: Int, name: String, missing: List<GoodsRef> = emptyList()) =
        CocktailMatch(id, name, matchedCount = 1, missingCount = missing.size, missingGoods = missing)

    fun editedMessage(data: String, step: CocktailsStep = CocktailsStep.MAIN) =
        (flow.onCallback(context(step), callback, data)!!.actions.first() as EditMessageAction).message

    beforeTest {
        clearMocks(suggestions, cards, ingredients, collections)
        filters.reset(7L)
        every { collections.nonAlcoholicCount("ru") } returns 0
        every { collections.favoritesCount(7L, "ru") } returns 0
        every { collections.marks(7L, any()) } returns CocktailMarks()
        every { ingredients.selectedCount(7L) } returns 4
        every { suggestions.counts(7L, "ru") } returns mapOf(0 to 3, 1 to 12, 2 to 0)
    }

    should("start with the sections that have cocktails") {
        val result = flow.start(FlowStartContext(tgUser, ru))

        result.stepKey shouldBe "main"
        val message = (result.actions.single() as SendMessageAction).message
        (message.model as CocktailsMainModel) shouldBe CocktailsMainModel(selected = 4, ready = 3, missingOne = 12, missingTwo = 0)
        message.inlineButtons.map { it.text } shouldContainExactly listOf(
            "buttons.cocktails.ready(3)",
            "buttons.cocktails.missing1(12)",
            "buttons.cocktails.random",
            "buttons.cocktails.buy",
            "buttons.cocktails.filter(0)",
            "buttons.cocktails.to_ingredients",
            "buttons.common.close",
        )
        message.inlineButtons.map { it.payload.data } shouldContainExactly listOf("S:0:0", "S:1:0", "R", "B", "FL", "OPEN", "CLOSE")
        message.inlineButtons.first { it.payload.data == "OPEN" }.payload.flow shouldBe "INGREDIENTS"
    }

    should("offer to open the ingredients and close when nothing can be suggested") {
        every { suggestions.counts(7L, "ru") } returns mapOf(0 to 0, 1 to 0, 2 to 0)
        every { ingredients.selectedCount(7L) } returns 0

        val message = (flow.start(FlowStartContext(tgUser, ru)).actions.single() as SendMessageAction).message

        message.inlineButtons.map { it.payload.data } shouldContainExactly listOf("FL", "OPEN", "CLOSE")
    }

    should("list a section with the missing ingredients and open buttons") {
        every { suggestions.findCocktails(7L, "ru", 1, 0, 8) } returns listOf(
            match(10, "Алфа", listOf(GoodsRef(3, "Ликёр"))),
            match(11, "Браво", listOf(GoodsRef(4, "Ром"), GoodsRef(5, "Джин"))),
        )

        val message = editedMessage("S:1:0")

        val model = message.model as CocktailsListModel
        model.title shouldBe "cocktails.section.missing1"
        model.total shouldBe 12
        model.pages shouldBe 2
        model.items shouldContainExactly listOf(CocktailsListItem(1, "Алфа", "Ликёр"), CocktailsListItem(2, "Браво", "Ром, Джин"))
        message.inlineButtons.map { it.payload.data } shouldContain "C:10:1:0"
        message.inlineButtons.map { it.payload.data } shouldContain "C:11:1:0"
        message.inlineButtons.map { it.payload.data } shouldContain "S:1:1"
        message.inlineButtons.map { it.payload.data } shouldContain "M"
    }

    should("mark cocktails shown in english and explain it") {
        every { suggestions.findCocktails(7L, "ru", 1, 0, 8) } returns listOf(
            match(10, "Алфа", listOf(GoodsRef(3, "Ликёр"))),
            CocktailMatch(11, "Bravo", 1, 1, listOf(GoodsRef(4, "Ром")), fallback = true),
        )

        val message = editedMessage("S:1:0")

        val model = message.model as CocktailsListModel
        model.items.map { it.fallback } shouldContainExactly listOf(false, true)
        model.hasFallback shouldBe true
        message.inlineButtons.map { it.text } shouldContain "buttons.cocktails.cocktail(Bravo ${CocktailsFlow.FALLBACK_FLAG})"
    }

    should("toggle filter tags on the filter screen and reset them") {
        val open = editedMessage("FL")
        open.inlineButtons.count { it.text.startsWith("buttons.common.unselected") } shouldBe CocktailTag.entries.size
        open.inlineButtons.none { it.text == "buttons.cocktails.filter.reset" } shouldBe true

        val toggled = editedMessage("T:SOUR", CocktailsStep.FILTER)
        toggled.inlineButtons.map { it.text } shouldContain "buttons.common.selected(cocktails.tag.sour)"
        toggled.inlineButtons.map { it.text } shouldContain "buttons.cocktails.filter.reset"
        filters.get(7L).tags shouldBe setOf(CocktailTag.SOUR)

        editedMessage("X", CocktailsStep.FILTER).inlineButtons.none { it.text == "buttons.cocktails.filter.reset" } shouldBe true
        filters.get(7L).isEmpty shouldBe true
        flow.onCallback(context(CocktailsStep.FILTER), callback, "T:UNKNOWN").shouldBeNull()
    }

    should("apply the chosen filter to the counts, sections and the random cocktail") {
        filters.toggle(7L, CocktailTag.STRONG)
        val filter = CocktailFilter(setOf(CocktailTag.STRONG))
        every { suggestions.counts(7L, "ru", filter) } returns mapOf(0 to 1, 1 to 2, 2 to 0)
        every { collections.nonAlcoholicCount("ru", filter) } returns 0
        every { collections.favoritesCount(7L, "ru", filter) } returns 0
        every { suggestions.findCocktails(7L, "ru", 1, 0, 8, filter) } returns listOf(match(10, "Алфа"))
        every { collections.randomReady(7L, "ru", any(), filter) } returns match(10, "Алфа")

        val main = editedMessage("M")
        (main.model as CocktailsMainModel).filter shouldBe "cocktails.tag.strong"
        main.inlineButtons.map { it.text } shouldContain "buttons.cocktails.filter.active(1)"
        val list = editedMessage("S:1:0").model as CocktailsListModel
        list.total shouldBe 2
        list.filter shouldBe "cocktails.tag.strong"
        every { cards.card(7L, "ru", 10) } returns CocktailCard(10, "Алфа", null, emptyList(), emptyList(), emptyList(), emptyList())
        editedMessage("R")
    }

    should("number cocktails continuously across pages") {
        every { suggestions.findCocktails(7L, "ru", 1, 1, 8) } returns listOf(match(20, "Чарли"))

        val model = editedMessage("S:1:1", CocktailsStep.LIST).model as CocktailsListModel

        model.page shouldBe 2
        model.items.single().number shouldBe 9
    }

    should("open a card and go back to the same page of the section") {
        every { cards.card(7L, "ru", 10) } returns CocktailCard(
            id = 10, name = "Алфа", author = "Автор", tags = listOf("крепкие", "кислые"),
            ingredients = listOf(
                CardIngredient("Водка", "50 мл", essential = true, have = true),
                CardIngredient("Ром", "20 мл", essential = true, have = false),
                CardIngredient("Лайм", "10 мл", essential = false, have = false),
            ),
            gadgets = listOf("Шейкер", "Стрейнер"),
            steps = listOf("Смешай"),
        )

        val message = editedMessage("C:10:1:2", CocktailsStep.LIST)

        val model = message.model as CocktailsCardModel
        model.ingredients shouldContainExactly listOf("✅ Водка — 50 мл", "❌ Ром — 20 мл", "• Лайм — 10 мл")
        model.tags shouldBe "крепкие · кислые"
        model.gadgets shouldBe "Шейкер, Стрейнер"
        model.hasMarks shouldBe true
        message.inlineButtons.map { it.payload.data } shouldContainExactly listOf("F:10:1:2", "K:10:1:2", "S:1:2")
        message.inlineButtons.map { it.text } shouldContain "buttons.cocktails.favorite.off"
    }

    should("name the substitute next to an ingredient available through it") {
        every { cards.card(7L, "ru", 10) } returns CocktailCard(
            id = 10, name = "Алфа", author = null, tags = emptyList(),
            ingredients = listOf(CardIngredient("Драй Оранж", "20 мл", essential = true, have = true, substitute = "Трипл сек")),
            gadgets = emptyList(), steps = emptyList(),
        )

        val model = editedMessage("C:10:0:0", CocktailsStep.LIST).model as CocktailsCardModel

        model.ingredients shouldContainExactly listOf("✅ Драй Оранж — 20 мл (cocktails.card.substitute(Трипл сек))")
    }

    should("fall back to the section when the cocktail does not exist in the locale") {
        every { cards.card(7L, "ru", 99) } returns null
        every { suggestions.findCocktails(7L, "ru", 0, 0, 8) } returns listOf(match(10, "Алфа"))

        val result = flow.onCallback(context(), callback, "C:99:0:0")!!

        result.stepKey shouldBe "list"
    }

    should("show non-alcoholic and favorites sections and the random button when there is something to show") {
        every { collections.nonAlcoholicCount("ru") } returns 41
        every { collections.favoritesCount(7L, "ru") } returns 2

        val buttons = (flow.start(FlowStartContext(tgUser, ru)).actions.single() as SendMessageAction).message.inlineButtons

        buttons.map { it.payload.data } shouldContainExactly listOf("S:0:0", "S:1:0", "S:10:0", "S:11:0", "R", "B", "FL", "OPEN", "CLOSE")
        buttons.map { it.text } shouldContain "buttons.cocktails.nonalcoholic(41)"
        buttons.map { it.text } shouldContain "buttons.cocktails.favorites(2)"
    }

    should("list non-alcoholic cocktails without missing ingredients and open their cards") {
        every { collections.nonAlcoholicCount("ru") } returns 41
        every { collections.nonAlcoholic("ru", 0, 8) } returns listOf(match(30, "Лимонад"))

        val message = editedMessage("S:10:0")

        val model = message.model as CocktailsListModel
        model.title shouldBe "cocktails.section.nonalcoholic"
        model.total shouldBe 41
        model.items shouldContainExactly listOf(CocktailsListItem(1, "Лимонад", ""))
        message.inlineButtons.map { it.payload.data } shouldContain "C:30:10:0"
        message.inlineButtons.map { it.payload.data } shouldContain "S:10:1"
    }

    should("list favorites of the user") {
        every { collections.favoritesCount(7L, "ru") } returns 1
        every { collections.favorites(7L, "ru", 0, 8) } returns listOf(match(10, "Алфа"))

        val model = editedMessage("S:11:0").model as CocktailsListModel

        model.title shouldBe "cocktails.section.favorites"
        model.items.single().name shouldBe "Алфа"
    }

    should("toggle favorite and cooked marks and redraw the card with the new state") {
        every { cards.card(7L, "ru", 10) } returns CocktailCard(10, "Алфа", null, emptyList(), emptyList(), emptyList(), emptyList())
        every { collections.toggleFavorite(7L, 10) } returns true
        every { collections.toggleCooked(7L, 10) } returns true
        every { collections.marks(7L, 10) } returns CocktailMarks(favorite = true, cooked = true)

        val favorite = editedMessage("F:10:0:1", CocktailsStep.CARD)
        val cooked = editedMessage("K:10:0:1", CocktailsStep.CARD)

        verify { collections.toggleFavorite(7L, 10) }
        verify { collections.toggleCooked(7L, 10) }
        favorite.inlineButtons.map { it.text } shouldContainExactly
            listOf("buttons.cocktails.favorite.on", "buttons.cocktails.cooked.on", "buttons.common.back")
        cooked.inlineButtons.map { it.payload.data } shouldContainExactly listOf("F:10:0:1", "K:10:0:1", "S:0:1")
    }

    should("show a random ready cocktail with a button for another one and back to the main screen") {
        every { collections.randomReady(7L, "ru", any()) } returns match(10, "Алфа")
        every { cards.card(7L, "ru", 10) } returns CocktailCard(10, "Алфа", null, emptyList(), emptyList(), emptyList(), emptyList())

        val message = editedMessage("R")

        (message.model as CocktailsCardModel).name shouldBe "Алфа"
        message.inlineButtons.map { it.payload.data } shouldContainExactly listOf("F:10:-1:0", "K:10:-1:0", "R", "M")
    }

    should("return to the main screen when nothing is ready for the random button") {
        every { collections.randomReady(7L, "ru", any()) } returns null

        flow.onCallback(context(), callback, "R")!!.stepKey shouldBe "main"
    }

    should("suggest what to buy") {
        every { suggestions.whatToBuy(7L, "ru", 5) } returns listOf(BuySuggestion(GoodsRef(3, "Ликёр"), 7))

        val model = editedMessage("B").model as CocktailsBuyModel

        model.items shouldContainExactly listOf(CocktailsBuyItem("Ликёр", 7))
    }

    should("not send anything when started from another flow") {
        flow.start(FlowStartContext(tgUser, ru, args = "OPEN")).actions shouldHaveSize 0
    }

    should("take over the message of the pressed button when opened from another flow") {
        val fromOtherFlow = mockk<CallbackQuery> {
            every { id } returns "cb2"
            every { message } returns mockk<MaybeInaccessibleMessage> { every { messageId } returns 77 }
        }

        val result = flow.onCallback(context(), fromOtherFlow, "OPEN")!!

        result.stepKey shouldBe "main"
        result.actions[0] shouldBe BindMessageAction(CocktailsFlow.MAIN_BINDING, 77)
        (result.actions[1] as EditMessageAction).message.model shouldBe CocktailsMainModel(4, 3, 12, 0)
    }

    should("delete the message and finish on close") {
        val result = flow.onCallback(context(), callback, "CLOSE")!!

        result.completed shouldBe true
        result.actions.filterIsInstance<DeleteMessageAction>() shouldHaveSize 1
    }

    should("only answer on a page label tap") {
        val result = flow.onCallback(context(CocktailsStep.LIST), callback, "NOOP")!!

        result.stepKey shouldBe "list"
        result.actions.single().let { it is AnswerCallbackAction } shouldBe true
    }

    should("ignore unknown or broken callbacks") {
        flow.onCallback(context(), callback, "WAT").shouldBeNull()
        flow.onCallback(context(), callback, "S:7:0").shouldBeNull()
        flow.onCallback(context(), callback, "S:x:0").shouldBeNull()
        flow.onCallback(context(), callback, "C:abc").shouldBeNull()
    }
})
