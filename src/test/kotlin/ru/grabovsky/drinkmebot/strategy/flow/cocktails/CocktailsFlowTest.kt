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
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import ru.grabovsky.drinkmebot.model.BuySuggestion
import ru.grabovsky.drinkmebot.model.CardIngredient
import ru.grabovsky.drinkmebot.model.CocktailCard
import ru.grabovsky.drinkmebot.model.CocktailMatch
import ru.grabovsky.drinkmebot.model.GoodsRef
import ru.grabovsky.drinkmebot.service.interfaces.CocktailCardService
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
import ru.grabovsky.drinkmebot.strategy.flow.FakeDataLocaleService
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.SendMessageAction
import java.util.Locale
import org.telegram.telegrambots.meta.api.objects.User as TgUser

class CocktailsFlowTest : ShouldSpec({
    val suggestions = mockk<CocktailSuggestService>()
    val cards = mockk<CocktailCardService>()
    val ingredients = mockk<IngredientService>()
    val i18n = object : I18nService {
        override fun i18n(code: String, locale: Locale, default: String?, vararg args: Any?) =
            if (args.isEmpty()) code else "$code(${args.joinToString(",")})"
    }
    val flow = CocktailsFlow(suggestions, cards, ingredients, i18n, FakeDataLocaleService(), mockk<StatsService>(relaxed = true))
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
        clearMocks(suggestions, cards, ingredients)
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
            "buttons.cocktails.buy",
            "buttons.cocktails.to_ingredients",
            "buttons.common.close",
        )
        message.inlineButtons.map { it.payload.data } shouldContainExactly listOf("S:0:0", "S:1:0", "B", "OPEN", "CLOSE")
        message.inlineButtons.first { it.payload.data == "OPEN" }.payload.flow shouldBe "INGREDIENTS"
    }

    should("offer to open the ingredients and close when nothing can be suggested") {
        every { suggestions.counts(7L, "ru") } returns mapOf(0 to 0, 1 to 0, 2 to 0)
        every { ingredients.selectedCount(7L) } returns 0

        val message = (flow.start(FlowStartContext(tgUser, ru)).actions.single() as SendMessageAction).message

        message.inlineButtons.map { it.payload.data } shouldContainExactly listOf("OPEN", "CLOSE")
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
        message.inlineButtons.single().payload.data shouldBe "S:1:2"
    }

    should("fall back to the section when the cocktail does not exist in the locale") {
        every { cards.card(7L, "ru", 99) } returns null
        every { suggestions.findCocktails(7L, "ru", 0, 0, 8) } returns listOf(match(10, "Алфа"))

        val result = flow.onCallback(context(), callback, "C:99:0:0")!!

        result.stepKey shouldBe "list"
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
