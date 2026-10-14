package ru.grabovsky.drinkmebot.strategy.flow.ingredients

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotBeBlank
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer
import ru.grabovsky.drinkmebot.config.FreemarkerConfig
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowKeys
import ru.grabovsky.drinkmebot.strategy.flow.core.templating.FlowTemplateRenderer
import java.util.Locale

class IngredientsTemplatesTest : ShouldSpec({
    val configurer: FreeMarkerConfigurer = FreemarkerConfig().freeMarkerConfigurer().also { it.afterPropertiesSet() }
    val renderer = FlowTemplateRenderer(configurer)
    val ru = Locale.forLanguageTag("ru")
    val en = Locale.forLanguageTag("en")
    val de = Locale.forLanguageTag("de")

    fun render(step: IngredientsStep, locale: Locale, model: Any?) = renderer.render(FlowKeys.INGREDIENTS, step.key, locale, model)

    should("render every step in ru and en") {
        listOf(ru, en).forEach { locale ->
            render(IngredientsStep.MAIN, locale, IngredientsMainModel(3)).shouldNotBeBlank()
            render(IngredientsStep.LIST, locale, IngredientsListModel("Водка", 1, 4, 1, 1, false)).shouldNotBeBlank()
            render(IngredientsStep.SEARCH_WAIT, locale, null).shouldNotBeBlank()
            render(IngredientsStep.RESET_CONFIRM, locale, IngredientsResetModel(5)).shouldNotBeBlank()
        }
    }

    should("use the language of the locale and fall back to english for unsupported ones") {
        render(IngredientsStep.MAIN, ru, IngredientsMainModel(3)) shouldContain "Мои ингредиенты"
        render(IngredientsStep.MAIN, en, IngredientsMainModel(3)) shouldContain "My ingredients"
        render(IngredientsStep.MAIN, de, IngredientsMainModel(3)) shouldContain "My ingredients"
    }

    should("escape html in the list title and show the number of pages") {
        val text = render(IngredientsStep.LIST, en, IngredientsListModel("<b>x</b>", 0, 25, 2, 3, false))

        text shouldContain "&lt;b&gt;x&lt;/b&gt;"
        text shouldContain "Page 2 of 3"
    }

    should("say that nothing was found") {
        render(IngredientsStep.LIST, en, IngredientsListModel("q", 0, 0, 1, 1, true)) shouldContain "Nothing found"
        render(IngredientsStep.LIST, ru, IngredientsListModel("q", 0, 0, 1, 1, true)) shouldContain "Ничего не найдено"
    }

    should("not format numbers with group separators") {
        render(IngredientsStep.MAIN, en, IngredientsMainModel(1234)) shouldContain "1234"
    }
})
