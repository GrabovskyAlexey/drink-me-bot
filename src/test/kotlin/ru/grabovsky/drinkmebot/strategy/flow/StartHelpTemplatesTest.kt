package ru.grabovsky.drinkmebot.strategy.flow

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotBeBlank
import io.mockk.every
import io.mockk.mockk
import ru.grabovsky.drinkmebot.config.FreemarkerConfig
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowKeys
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowParseMode
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowStartContext
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.SendMessageAction
import ru.grabovsky.drinkmebot.strategy.flow.core.templating.FlowTemplateRenderer
import ru.grabovsky.drinkmebot.strategy.flow.help.HelpFlow
import ru.grabovsky.drinkmebot.strategy.flow.start.StartFlow
import ru.grabovsky.drinkmebot.strategy.flow.start.StartViewModel
import java.util.Locale
import org.telegram.telegrambots.meta.api.objects.User as TgUser

class StartHelpTemplatesTest : ShouldSpec({
    val renderer = FlowTemplateRenderer(FreemarkerConfig().freeMarkerConfigurer().also { it.afterPropertiesSet() })
    val ru = Locale.forLanguageTag("ru")
    val en = Locale.forLanguageTag("en")
    val user = mockk<TgUser> {
        every { userName } returns "a_b*[x]<i>"
        every { firstName } returns "Имя"
    }

    should("send start and help as html") {
        val start = StartFlow(EchoI18n).start(FlowStartContext(user, ru)).actions.single() as SendMessageAction
        val help = HelpFlow().start(FlowStartContext(user, ru)).actions.single() as SendMessageAction

        start.message.parseMode shouldBe FlowParseMode.HTML
        help.message.parseMode shouldBe FlowParseMode.HTML
    }

    should("offer buttons that open ingredients and cocktails in the same message") {
        val start = StartFlow(EchoI18n).start(FlowStartContext(user, ru)).actions.single() as SendMessageAction

        start.message.inlineButtons.map { it.payload.flow to it.payload.data } shouldBe
            listOf("INGREDIENTS" to "OPEN", "COCKTAILS" to "OPEN")
    }

    should("escape the user name in the greeting so that special characters do not break the message") {
        val text = renderer.render(FlowKeys.START, "main", ru, StartViewModel("a_b*[x]<i>"))

        text shouldContain "<b>a_b*[x]&lt;i&gt;</b>"
    }

    should("greet without a name") {
        renderer.render(FlowKeys.START, "main", en, null) shouldContain "Hi!"
    }

    should("render help in both languages") {
        renderer.render(FlowKeys.HELP, "main", ru, null).shouldNotBeBlank()
        renderer.render(FlowKeys.HELP, "main", en, null) shouldContain "How it works"
    }
})
