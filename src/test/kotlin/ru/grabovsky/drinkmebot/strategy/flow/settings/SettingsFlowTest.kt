package ru.grabovsky.drinkmebot.strategy.flow.settings

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import ru.grabovsky.drinkmebot.entity.User
import ru.grabovsky.drinkmebot.entity.UserProfile
import ru.grabovsky.drinkmebot.service.interfaces.UserService
import ru.grabovsky.drinkmebot.strategy.flow.EchoI18n
import ru.grabovsky.drinkmebot.strategy.flow.FakeDataLocaleService
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.DeleteMessageAction
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.EditMessageAction
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowContext
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowMessage
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowStartContext
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowStateHolder
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.SendMessageAction
import java.util.Locale
import org.telegram.telegrambots.meta.api.objects.User as TgUser

class SettingsFlowTest : ShouldSpec({
    val userService = mockk<UserService>(relaxed = true)
    val dataLocale = FakeDataLocaleService()
    val flow = SettingsFlow(userService, dataLocale, EchoI18n)
    val ru = Locale.forLanguageTag("ru")
    val en = Locale.forLanguageTag("en")
    val tgUser = mockk<TgUser> { every { id } returns 7L }
    val callback = mockk<CallbackQuery> { every { id } returns "cb1" }
    lateinit var user: User

    fun context(locale: Locale = ru) =
        FlowContext(tgUser, locale, FlowStateHolder(SettingsStep.MAIN.key, SettingsStateData(), mapOf(SettingsFlow.MAIN_BINDING to 1)))

    fun startMessage(locale: Locale = ru): FlowMessage =
        (flow.start(FlowStartContext(tgUser, locale)).actions.single() as SendMessageAction).message

    fun pressed(data: String, locale: Locale = ru): FlowMessage =
        (flow.onCallback(context(locale), callback, data)!!.actions.first() as EditMessageAction).message

    beforeTest {
        clearMocks(userService)
        dataLocale.choose(7L, null)
        user = User(7L, "T", null, "t").apply { profile = UserProfile(user = this) }
        every { userService.getUser(7L) } returns user
    }

    should("show eight flags in two rows right on the main screen, then auto-delete and close") {
        val buttons = startMessage().inlineButtons

        buttons.map { it.payload.data } shouldContainExactly
            listOf("SL:ru", "SL:ua", "SL:us", "SL:uk", "SL:de", "SL:es", "SL:fr", "SL:it", "CLEAN", "CLOSE")
        buttons.take(8).map { it.row to it.col } shouldContainExactly
            listOf(0 to 0, 0 to 1, 0 to 2, 0 to 3, 1 to 0, 1 to 1, 1 to 2, 1 to 3)
        buttons.take(8).map { it.text.removePrefix("✅ ") } shouldContainExactly
            listOf("🇷🇺", "🇺🇦", "🇺🇸", "🇬🇧", "🇩🇪", "🇪🇸", "🇫🇷", "🇮🇹")
    }

    should("offer to return to the Telegram language only after a language is chosen") {
        dataLocale.choose(7L, "de")

        val buttons = startMessage().inlineButtons

        buttons.map { it.payload.data }.takeLast(3) shouldContainExactly listOf("AUTO", "CLEAN", "CLOSE")
        buttons.single { it.payload.data == "AUTO" }.row shouldBe 2
        buttons.single { it.payload.data == "CLEAN" }.row shouldBe 3
    }

    should("reset the chosen language and redraw in the Telegram language") {
        dataLocale.choose(7L, "de")

        val result = flow.onCallback(context(en), callback, "AUTO")!!

        dataLocale.chosen(7L).shouldBeNull()
        (result.actions.first() as EditMessageAction).message.inlineButtons.none { it.payload.data == "AUTO" } shouldBe true
    }

    should("mark the language detected from Telegram while nothing is chosen") {
        startMessage(ru).inlineButtons.filter { it.text.startsWith("✅") }.map { it.payload.data } shouldContainExactly listOf("SL:ru")
        startMessage(en).inlineButtons.filter { it.text.startsWith("✅") }.map { it.payload.data } shouldContainExactly listOf("SL:us")
    }

    should("mark the chosen language and show its name") {
        dataLocale.choose(7L, "de")

        val message = startMessage()

        message.inlineButtons.filter { it.text.startsWith("✅") }.map { it.text } shouldContainExactly listOf("✅ 🇩🇪")
        (message.model as SettingsMainModel).language shouldBe "settings.language.de"
    }

    should("choose a language and redraw the message in the new interface language") {
        val result = flow.onCallback(context(ru), callback, "SL:de")!!
        val message = (result.actions.first() as EditMessageAction).message

        dataLocale.chosen(7L) shouldBe "de"
        message.locale shouldBe en
        message.inlineButtons.filter { it.text.startsWith("✅") }.map { it.payload.data } shouldContainExactly listOf("SL:de")
    }

    should("switch the interface back to russian when russian is chosen") {
        dataLocale.choose(7L, "fr")

        pressed("SL:ru", en).locale shouldBe ru
        dataLocale.chosen(7L) shouldBe "ru"
    }

    should("ignore an unknown or missing language code") {
        flow.onCallback(context(), callback, "SL:xx").shouldBeNull()
        flow.onCallback(context(), callback, "SL").shouldBeNull()
        flow.onCallback(context(), callback, "SL:AUTO").shouldBeNull()
    }

    should("toggle auto-delete and save the profile") {
        val message = pressed("CLEAN")

        user.profile!!.settings.cleanChat shouldBe false
        verify { userService.saveUser(user) }
        message.inlineButtons.first { it.payload.data == "CLEAN" }.text shouldBe "buttons.settings.clean.off"
    }

    should("delete the message and finish on close") {
        val result = flow.onCallback(context(), callback, "CLOSE")!!

        result.completed shouldBe true
        result.actions.filterIsInstance<DeleteMessageAction>().size shouldBe 1
    }

    should("ignore unknown callbacks") {
        flow.onCallback(context(), callback, "WAT").shouldBeNull()
        flow.onCallback(context(), callback, "L").shouldBeNull()
    }
})
