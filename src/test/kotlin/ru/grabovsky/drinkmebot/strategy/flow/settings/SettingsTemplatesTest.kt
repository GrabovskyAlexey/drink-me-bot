package ru.grabovsky.drinkmebot.strategy.flow.settings

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.string.shouldContain
import ru.grabovsky.drinkmebot.config.FreemarkerConfig
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowKeys
import ru.grabovsky.drinkmebot.strategy.flow.core.templating.FlowTemplateRenderer
import java.util.Locale

class SettingsTemplatesTest : ShouldSpec({
    val renderer = FlowTemplateRenderer(FreemarkerConfig().freeMarkerConfigurer().also { it.afterPropertiesSet() })
    val model = SettingsMainModel("🇩🇪 Deutsch")

    should("show the current language in ru, en and an unsupported locale") {
        listOf("ru", "en", "de").forEach {
            renderer.render(FlowKeys.SETTINGS, "main", Locale.forLanguageTag(it), model) shouldContain "🇩🇪 Deutsch"
        }
    }

    should("use the language of the locale and fall back to english") {
        renderer.render(FlowKeys.SETTINGS, "main", Locale.forLanguageTag("ru"), model) shouldContain "Язык:"
        renderer.render(FlowKeys.SETTINGS, "main", Locale.forLanguageTag("en"), model) shouldContain "Language:"
        renderer.render(FlowKeys.SETTINGS, "main", Locale.forLanguageTag("it"), model) shouldContain "Language:"
    }
})
