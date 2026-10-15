package ru.grabovsky.drinkmebot.strategy.flow

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.message.MaybeInaccessibleMessage
import ru.grabovsky.drinkmebot.model.CategorySummary
import ru.grabovsky.drinkmebot.model.IngredientCategory
import ru.grabovsky.drinkmebot.service.interfaces.CocktailCardService
import ru.grabovsky.drinkmebot.service.interfaces.CocktailCollectionService
import ru.grabovsky.drinkmebot.service.interfaces.CocktailSuggestService
import ru.grabovsky.drinkmebot.service.interfaces.I18nService
import ru.grabovsky.drinkmebot.service.interfaces.IngredientService
import ru.grabovsky.drinkmebot.service.interfaces.StatsService
import ru.grabovsky.drinkmebot.strategy.flow.cocktails.CocktailsFlow
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.BindMessageAction
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.EditMessageAction
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowAction
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowActionExecutor
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowBindingsMutation
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowEngine
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowKeys
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowStateSnapshot
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.SendMessageAction
import ru.grabovsky.drinkmebot.strategy.flow.core.serialization.JacksonFlowPayloadSerializer
import ru.grabovsky.drinkmebot.service.interfaces.FlowStateService
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowKey
import ru.grabovsky.drinkmebot.strategy.flow.ingredients.IngredientsFlow
import java.util.Locale
import org.telegram.telegrambots.meta.api.objects.User as TgUser

/** Переход между /cocktails и /ingredients по кнопке: сообщение с кнопкой переходит к другому flow, новых сообщений нет. */
class FlowNavigationTest : ShouldSpec({
    val ru = Locale.forLanguageTag("ru")
    val user = mockk<TgUser> { every { id } returns 7L }

    val states = mutableMapOf<Pair<Long, FlowKey>, FlowStateSnapshot>()
    val stateService = object : FlowStateService {
        override fun load(userId: Long, flowKey: FlowKey) = states[userId to flowKey]
        override fun save(snapshot: FlowStateSnapshot) { states[snapshot.userId to snapshot.flowKey] = snapshot }
        override fun clear(userId: Long, flowKey: FlowKey) { states.remove(userId to flowKey) }
        override fun findListFlow(userId: Long) = null
    }

    val executed = mutableListOf<FlowAction>()
    val executor = object : FlowActionExecutor {
        override fun execute(user: TgUser, locale: Locale, currentBindings: Map<String, Int>, actions: List<FlowAction>): FlowBindingsMutation {
            executed += actions
            val bound = actions.filterIsInstance<BindMessageAction>().associate { it.bindingKey to it.messageId }
            return FlowBindingsMutation(replacements = bound)
        }
    }

    val i18n = object : I18nService {
        override fun i18n(code: String, locale: Locale, default: String?, vararg args: Any?) = code
    }
    val ingredients = mockk<IngredientService> {
        every { categories(7L) } returns listOf(CategorySummary(IngredientCategory.VODKA, 4, 1))
        every { selectedCount(7L) } returns 1
    }
    val suggestions = mockk<CocktailSuggestService> { every { counts(7L, "ru") } returns mapOf(0 to 1, 1 to 0, 2 to 0) }
    val engine = FlowEngine(
        listOf(
            IngredientsFlow(ingredients, i18n, FakeDataLocaleService()),
            CocktailsFlow(suggestions, mockk<CocktailCardService>(), mockk<CocktailCollectionService>(relaxed = true), ingredients, i18n, FakeDataLocaleService(), mockk<StatsService>(relaxed = true)),
        ),
        stateService, JacksonFlowPayloadSerializer(jacksonObjectMapper()), executor,
    )

    fun callbackFromMessage(messageId: Int) = mockk<CallbackQuery> {
        every { id } returns "cb"
        every { message } returns mockk<MaybeInaccessibleMessage> { every { this@mockk.messageId } returns messageId }
    }

    /** Повторяет логику `ReceiverServiceImpl.processCallback` для callback-а в чужой flow. */
    fun press(flowKey: FlowKey, data: String, messageId: Int) {
        executed.clear()
        val callback = callbackFromMessage(messageId)
        if (engine.onCallback(flowKey, user, ru, callback, data)) return
        engine.start(flowKey, user, ru, data)
        engine.onCallback(flowKey, user, ru, callback, data) shouldBe true
    }

    should("open ingredients from the cocktails message without sending a new message") {
        engine.start(FlowKeys.COCKTAILS, user, ru)
        executed.clear()
        states.remove(7L to FlowKeys.INGREDIENTS)

        press(FlowKeys.INGREDIENTS, "OPEN", messageId = 100)

        executed.filterIsInstance<SendMessageAction>() shouldBe emptyList()
        executed.filterIsInstance<BindMessageAction>() shouldBe listOf(BindMessageAction("main", 100))
        executed.filterIsInstance<EditMessageAction>().single().message.flowKey shouldBe FlowKeys.INGREDIENTS
        states.getValue(7L to FlowKeys.INGREDIENTS).messageBindings shouldBe mapOf("main" to 100)
    }

    should("go back to cocktails in the same message and keep editing it") {
        press(FlowKeys.COCKTAILS, "OPEN", messageId = 100)

        executed.filterIsInstance<SendMessageAction>() shouldBe emptyList()
        executed.filterIsInstance<EditMessageAction>().single().message.flowKey shouldBe FlowKeys.COCKTAILS
        states.getValue(7L to FlowKeys.COCKTAILS).messageBindings shouldBe mapOf("main" to 100)
    }

    should("rebind to the new message when the flow already has an old state") {
        press(FlowKeys.INGREDIENTS, "OPEN", messageId = 250)

        states.getValue(7L to FlowKeys.INGREDIENTS).messageBindings shouldBe mapOf("main" to 250)
    }
})
