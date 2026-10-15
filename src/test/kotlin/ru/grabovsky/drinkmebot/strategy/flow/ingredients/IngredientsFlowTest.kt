package ru.grabovsky.drinkmebot.strategy.flow.ingredients

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.grabovsky.drinkmebot.model.CategorySummary
import ru.grabovsky.drinkmebot.model.IngredientCategory
import ru.grabovsky.drinkmebot.model.IngredientItem
import ru.grabovsky.drinkmebot.service.interfaces.I18nService
import ru.grabovsky.drinkmebot.service.interfaces.IngredientService
import org.telegram.telegrambots.meta.api.objects.message.MaybeInaccessibleMessage
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.AnswerCallbackAction
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.BindMessageAction
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.DeleteMessageAction
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.DeleteMessageIdAction
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.EditMessageAction
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowContext
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowStartContext
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowStateHolder
import ru.grabovsky.drinkmebot.strategy.flow.FakeDataLocaleService
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.SendMessageAction
import java.util.Locale
import org.telegram.telegrambots.meta.api.objects.User as TgUser

class IngredientsFlowTest : ShouldSpec({
    val ingredients = mockk<IngredientService>()
    val i18n = object : I18nService {
        override fun i18n(code: String, locale: Locale, default: String?, vararg args: Any?) =
            if (args.isEmpty()) code else "$code(${args.joinToString(",")})"
    }
    val flow = IngredientsFlow(ingredients, i18n, FakeDataLocaleService())
    val ru = Locale.forLanguageTag("ru")
    val tgUser = mockk<TgUser> { every { id } returns 7L }
    val callback = mockk<CallbackQuery> { every { id } returns "cb1" }

    fun context(step: IngredientsStep = IngredientsStep.MAIN, state: IngredientsState = IngredientsState()) =
        FlowContext(tgUser, ru, FlowStateHolder(step.key, state, mapOf(IngredientsFlow.MAIN_BINDING to 1)))

    fun items(count: Int, selected: Set<Int> = emptySet()) = (1..count).map { IngredientItem(it, "Item $it", it in selected) }

    beforeTest {
        clearMocks(ingredients)
        every { ingredients.categories(7L) } returns listOf(
            CategorySummary(IngredientCategory.VODKA, 4, 1),
            CategorySummary(IngredientCategory.WHISKY, 7, 0),
            CategorySummary(IngredientCategory.RUM, 7, 2),
        )
        every { ingredients.selectedCount(7L) } returns 3
    }

    should("start with the category screen") {
        val result = flow.start(FlowStartContext(tgUser, ru))

        result.stepKey shouldBe "main"
        val message = (result.actions.single() as SendMessageAction).message
        (message.model as IngredientsMainModel).selected shouldBe 3
        message.inlineButtons.map { it.text }.take(3) shouldContainExactly listOf(
            "buttons.ingredients.category(ingredients.category.vodka,1,4)",
            "buttons.ingredients.category(ingredients.category.whisky,0,7)",
            "buttons.ingredients.category(ingredients.category.rum,2,7)",
        )
        message.inlineButtons.map { it.payload.data } shouldContain "SEARCH"
        message.inlineButtons.map { it.payload.data } shouldContain "RESET"
        message.inlineButtons.first { it.payload.data == "OPEN" }.payload.flow shouldBe "COCKTAILS"
    }

    should("not send anything when started from another flow") {
        flow.start(FlowStartContext(tgUser, ru, args = "OPEN")).actions shouldHaveSize 0
    }

    should("take over the message of the pressed button when opened from another flow") {
        val fromOtherFlow = mockk<CallbackQuery> {
            every { id } returns "cb2"
            every { message } returns mockk<MaybeInaccessibleMessage> { every { messageId } returns 77 }
        }

        val result = flow.onCallback(context(IngredientsStep.LIST), fromOtherFlow, "OPEN")!!

        result.stepKey shouldBe "main"
        result.actions[0] shouldBe BindMessageAction(IngredientsFlow.MAIN_BINDING, 77)
        (result.actions[1] as EditMessageAction).message.model shouldBe IngredientsMainModel(3)
    }

    should("open a category with the user marks and paginate it") {
        every { ingredients.items(7L, "ru", IngredientCategory.VODKA) } returns items(12, selected = setOf(2))

        val result = flow.onCallback(context(), callback, "V:VODKA:0")!!

        result.stepKey shouldBe "list"
        val message = (result.actions.first() as EditMessageAction).message
        val model = message.model as IngredientsListModel
        model.total shouldBe 12
        model.selected shouldBe 1
        model.pages shouldBe 2
        message.inlineButtons.filter { it.payload.data.startsWith("T:") } shouldHaveSize 10
        message.inlineButtons.first { it.payload.data == "T:2:VODKA:0" }.text shouldBe "buttons.common.selected(Item 2)"
        message.inlineButtons.first { it.payload.data == "T:1:VODKA:0" }.text shouldBe "buttons.common.unselected(Item 1)"
        message.inlineButtons.map { it.payload.data } shouldContain "V:VODKA:1"
        result.actions.last().shouldBeInstanceOf<AnswerCallbackAction>()
    }

    should("show the second page") {
        every { ingredients.items(7L, "ru", IngredientCategory.VODKA) } returns items(12)

        val message = (flow.onCallback(context(IngredientsStep.LIST), callback, "V:VODKA:1")!!.actions.first() as EditMessageAction).message

        message.inlineButtons.filter { it.payload.data.startsWith("T:") } shouldHaveSize 2
        message.inlineButtons.map { it.payload.data } shouldContain "V:VODKA:0"
    }

    should("toggle an ingredient and redraw the same page") {
        every { ingredients.toggle(7L, 3) } returns true
        every { ingredients.items(7L, "ru", IngredientCategory.RUM) } returns items(5, selected = setOf(3))

        val result = flow.onCallback(context(IngredientsStep.LIST), callback, "T:3:RUM:0")!!

        verify { ingredients.toggle(7L, 3) }
        val message = (result.actions.first() as EditMessageAction).message
        message.inlineButtons.first { it.payload.data == "T:3:RUM:0" }.text shouldBe "buttons.common.selected(Item 3)"
    }

    should("warn when the ingredient cannot be selected") {
        every { ingredients.toggle(7L, 999) } returns null

        val answer = flow.onCallback(context(IngredientsStep.LIST), callback, "T:999:RUM:0")!!.actions.single() as AnswerCallbackAction

        answer.text shouldBe "alerts.ingredients.unavailable"
        answer.showAlert shouldBe true
    }

    should("ask for a search query and then show results, deleting the user message") {
        val prompt = flow.onCallback(context(), callback, "SEARCH")!!
        prompt.stepKey shouldBe "search_wait"

        every { ingredients.search(7L, "ru", "ром") } returns items(2)
        val message = mockk<Message> {
            every { text } returns "  ром "
            every { messageId } returns 55
        }

        val result = flow.onMessage(context(IngredientsStep.SEARCH_WAIT), message)!!

        result.stepKey shouldBe "list"
        result.payload.query shouldBe "ром"
        result.actions.filterIsInstance<DeleteMessageIdAction>().single().messageId shouldBe 55
        val model = (result.actions.first() as EditMessageAction).message.model as IngredientsListModel
        model.title shouldBe "ingredients.search_title(ром)"
        model.total shouldBe 2
    }

    should("ignore text messages outside of the search prompt") {
        val message = mockk<Message> { every { text } returns "ром" }

        flow.onMessage(context(IngredientsStep.MAIN), message).shouldBeNull()
        flow.onMessage(context(IngredientsStep.LIST), message).shouldBeNull()
    }

    should("return to categories when the search state is lost") {
        val result = flow.onCallback(context(IngredientsStep.LIST, IngredientsState(null)), callback, "V:S:0")!!

        result.stepKey shouldBe "main"
    }

    should("ask for confirmation before clearing and clear only on confirmation") {
        val confirm = flow.onCallback(context(), callback, "RESET")!!
        confirm.stepKey shouldBe "reset_confirm"
        verify(exactly = 0) { ingredients.reset(any()) }

        every { ingredients.reset(7L) } returns 3
        val done = flow.onCallback(context(IngredientsStep.RESET_CONFIRM), callback, "RESET_YES")!!

        verify { ingredients.reset(7L) }
        done.stepKey shouldBe "main"
    }

    should("delete the message and finish on close") {
        val result = flow.onCallback(context(), callback, "CLOSE")!!

        result.completed shouldBe true
        result.actions.filterIsInstance<DeleteMessageAction>() shouldHaveSize 1
    }

    should("show categories one per row, five per page") {
        every { ingredients.categories(7L) } returns IngredientCategory.entries.map { CategorySummary(it, 3, 0) }

        val first = flow.start(FlowStartContext(tgUser, ru)).actions.filterIsInstance<SendMessageAction>().single().message
        val categoryButtons = first.inlineButtons.filter { it.payload.data.startsWith("V:") }

        categoryButtons.map { it.payload.data } shouldContainExactly
            listOf("V:VODKA:0", "V:WHISKY:0", "V:RUM:0", "V:GIN:0", "V:TEQUILA:0")
        categoryButtons.map { it.row } shouldContainExactly listOf(0, 1, 2, 3, 4)
        first.inlineButtons.map { it.payload.data } shouldContain "M:1"
    }

    should("show the second page of categories") {
        every { ingredients.categories(7L) } returns IngredientCategory.entries.map { CategorySummary(it, 3, 0) }

        val message = (flow.onCallback(context(), callback, "M:1")!!.actions.first() as EditMessageAction).message

        message.inlineButtons.filter { it.payload.data.startsWith("V:") }.map { it.payload.data } shouldContainExactly
            listOf("V:BRANDY:0", "V:LIQUEUR:0", "V:WINE:0", "V:BITTER:0", "V:SYRUP:0")
        message.inlineButtons.map { it.payload.data } shouldContain "M:0"
        message.inlineButtons.map { it.payload.data } shouldContain "M:2"
    }

    should("show the last category on the third page") {
        every { ingredients.categories(7L) } returns IngredientCategory.entries.map { CategorySummary(it, 3, 0) }

        val message = (flow.onCallback(context(), callback, "M:2")!!.actions.first() as EditMessageAction).message

        message.inlineButtons.filter { it.payload.data.startsWith("V:") }.map { it.payload.data } shouldContainExactly listOf("V:OTHER:0")
    }

    should("go back to the categories page that contains the opened category") {
        every { ingredients.items(7L, "ru", IngredientCategory.LIQUEUR) } returns items(3)
        every { ingredients.items(7L, "ru", IngredientCategory.RUM) } returns items(3)

        fun backOf(view: String): String {
            val message = (flow.onCallback(context(), callback, "V:$view:0")!!.actions.first() as EditMessageAction).message
            return message.inlineButtons.single { it.row == 99 }.payload.data
        }

        backOf("LIQUEUR") shouldBe "M:1"
        backOf("RUM") shouldBe "M:0"
    }

    should("keep supporting the old MAIN callback") {
        flow.onCallback(context(), callback, "MAIN")!!.stepKey shouldBe "main"
    }

    should("ignore unknown callbacks") {
        flow.onCallback(context(), callback, "WAT:1").shouldBeNull()
        flow.onCallback(context(), callback, "V:UNKNOWN:0").shouldBeNull()
    }
})
