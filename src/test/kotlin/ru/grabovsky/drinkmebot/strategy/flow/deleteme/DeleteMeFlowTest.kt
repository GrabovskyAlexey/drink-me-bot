package ru.grabovsky.drinkmebot.strategy.flow.deleteme

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import ru.grabovsky.drinkmebot.config.FreemarkerConfig
import ru.grabovsky.drinkmebot.service.interfaces.I18nService
import ru.grabovsky.drinkmebot.service.interfaces.UserDataService
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.*
import ru.grabovsky.drinkmebot.strategy.flow.core.templating.FlowTemplateRenderer
import java.util.Locale
import org.telegram.telegrambots.meta.api.objects.User as TgUser

class DeleteMeFlowTest : ShouldSpec({
    val ru = Locale.forLanguageTag("ru")
    val data = mockk<UserDataService>(relaxed = true) { every { export(any(), any()) } returns "{}".toByteArray() }
    val i18n = object : I18nService {
        override fun i18n(code: String, locale: Locale, default: String?, vararg args: Any?) = code
    }
    val flow = DeleteMeFlow(data, i18n)
    val callback = mockk<CallbackQuery> {
        every { id } returns "cb1"
        every { from } returns mockk<TgUser> { every { id } returns 7L }
    }

    fun context(step: DeleteMeStep) =
        FlowContext(mockk(), ru, FlowStateHolder(step.key, DeleteMeState(), emptyMap()))

    should("not delete anything on start and ask") {
        val start = flow.start(FlowStartContext(mockk(), ru))
        start.stepKey shouldBe DeleteMeStep.MENU.key
        val ask = flow.onCallback(context(DeleteMeStep.MENU), callback, "ASK")!!
        ask.stepKey shouldBe DeleteMeStep.CONFIRM.key
        verify(exactly = 0) { data.deleteAll(any()) }
    }

    should("delete only from the confirm step") {
        val outdated = flow.onCallback(context(DeleteMeStep.MENU), callback, "DO")!!
        outdated.completed shouldBe false
        verify(exactly = 0) { data.deleteAll(any()) }

        val done = flow.onCallback(context(DeleteMeStep.CONFIRM), callback, "DO")!!
        done.completed shouldBe true
        verify(exactly = 1) { data.deleteAll(7L) }
    }

    should("send the export as a document") {
        val result = flow.onCallback(context(DeleteMeStep.MENU), callback, "EXPORT")!!
        result.actions.first().shouldBeInstanceOf<SendDocumentAction>()
    }

    should("render menu, confirm and done in ru and en") {
        val renderer = FlowTemplateRenderer(FreemarkerConfig().freeMarkerConfigurer().also { it.afterPropertiesSet() })
        renderer.render(FlowKeys.DELETE_ME, "menu", ru) shouldContain "Мои данные"
        renderer.render(FlowKeys.DELETE_ME, "confirm", Locale.ENGLISH) shouldContain "cannot be undone"
        renderer.render(FlowKeys.DELETE_ME, "done", Locale.forLanguageTag("de")) shouldContain "deleted"
    }
})
