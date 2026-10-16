package ru.grabovsky.drinkmebot.strategy.flow.shopping

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import ru.grabovsky.drinkmebot.model.ShoppingItem
import ru.grabovsky.drinkmebot.service.interfaces.I18nService
import ru.grabovsky.drinkmebot.service.interfaces.ShoppingListService
import ru.grabovsky.drinkmebot.strategy.flow.FakeDataLocaleService
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.AnswerCallbackAction
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.DeleteMessageAction
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.EditMessageAction
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowContext
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowMessage
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowStartContext
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowStateHolder
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.SendMessageAction
import java.util.Locale
import org.telegram.telegrambots.meta.api.objects.User as TgUser

class ShoppingFlowTest : ShouldSpec({
    val shopping = mockk<ShoppingListService>()
    val i18n = object : I18nService {
        override fun i18n(code: String, locale: Locale, default: String?, vararg args: Any?) =
            if (args.isEmpty()) code else "$code(${args.joinToString(",")})"
    }
    val flow = ShoppingFlow(shopping, i18n, FakeDataLocaleService())
    val ru = Locale.forLanguageTag("ru")
    val tgUser = mockk<TgUser> { every { id } returns 7L }
    val callback = mockk<CallbackQuery> { every { id } returns "cb1" }

    fun context() = FlowContext(tgUser, ru, FlowStateHolder(ShoppingStep.MAIN.key, ShoppingState(), mapOf(ShoppingFlow.MAIN_BINDING to 1)))

    fun pressed(data: String) = flow.onCallback(context(), callback, data)!!

    fun edited(data: String): FlowMessage = (pressed(data).actions.first() as EditMessageAction).message

    beforeTest {
        clearMocks(shopping)
        every { shopping.items(7L, "ru") } returns listOf(ShoppingItem(1, "Ром", false), ShoppingItem(2, "Водка", true))
    }

    should("show items as buttons with bought marks and the list actions") {
        val message = (flow.start(FlowStartContext(tgUser, ru)).actions.single() as SendMessageAction).message

        message.inlineButtons.map { it.text } shouldContainExactly
            listOf("⬜ Ром", "✅ Водка", "buttons.shopping.done", "buttons.shopping.clear", "buttons.common.close")
        (message.model as ShoppingMainModel).let { it.total to it.bought } shouldBe (2 to 1)
    }

    should("offer only close for an empty list") {
        every { shopping.items(7L, "ru") } returns emptyList()

        edited("P:0").inlineButtons.map { it.text } shouldContainExactly listOf("buttons.common.close")
    }

    should("paginate long lists") {
        every { shopping.items(7L, "ru") } returns (1..12).map { ShoppingItem(it, "Т$it", false) }

        val message = edited("P:1")

        (message.model as ShoppingMainModel).let { it.page to it.pages } shouldBe (2 to 2)
        message.inlineButtons.filter { it.payload.data.startsWith("T:") }.map { it.payload.data } shouldContainExactly listOf("T:11:1", "T:12:1")
    }

    should("toggle bought and stay on the page") {
        every { shopping.toggleBought(7L, 1) } returns true

        edited("T:1:0")

        verify { shopping.toggleBought(7L, 1) }
    }

    should("move bought items to ingredients and report how many") {
        every { shopping.moveBoughtToIngredients(7L) } returns 1

        (pressed("D").actions.last() as AnswerCallbackAction).text shouldBe "alerts.shopping.moved(1)"
    }

    should("clear the list") {
        every { shopping.clear(7L) } returns 2

        edited("CLR")

        verify { shopping.clear(7L) }
    }

    should("delete the message and finish on close") {
        val result = pressed("CLOSE")

        result.completed shouldBe true
        result.actions.filterIsInstance<DeleteMessageAction>().size shouldBe 1
    }

    should("ignore unknown callbacks") {
        flow.onCallback(context(), callback, "WAT") shouldBe null
        flow.onCallback(context(), callback, "T") shouldBe null
    }
})
