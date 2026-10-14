package ru.grabovsky.drinkmebot.strategy.flow.cocktails

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotBeBlank
import io.kotest.matchers.string.shouldNotContain
import ru.grabovsky.drinkmebot.config.FreemarkerConfig
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowKeys
import ru.grabovsky.drinkmebot.strategy.flow.core.templating.FlowTemplateRenderer
import java.util.Locale

class CocktailsTemplatesTest : ShouldSpec({
    val renderer = FlowTemplateRenderer(FreemarkerConfig().freeMarkerConfigurer().also { it.afterPropertiesSet() })
    val ru = Locale.forLanguageTag("ru")
    val en = Locale.forLanguageTag("en")
    val de = Locale.forLanguageTag("de")

    fun render(step: CocktailsStep, locale: Locale, model: Any?) = renderer.render(FlowKeys.COCKTAILS, step.key, locale, model)

    val card = CocktailsCardModel(
        name = "Мартини <b>", author = "Автор", tags = "крепкие · кислые",
        ingredients = listOf("✅ Водка — 50 мл", "❌ Ром — 20 мл"), gadgets = "Шейкер, Стрейнер",
        steps = listOf("Смешай", "Налей"), hasMarks = true,
    )

    should("render every step in ru, en and an unsupported locale") {
        listOf(ru, en, de).forEach { locale ->
            render(CocktailsStep.MAIN, locale, CocktailsMainModel(3, 1, 2, 3)).shouldNotBeBlank()
            render(CocktailsStep.LIST, locale, CocktailsListModel("T", 12, 1, 2, listOf(CocktailsListItem(1, "A", "B, C")))).shouldNotBeBlank()
            render(CocktailsStep.CARD, locale, card).shouldNotBeBlank()
            render(CocktailsStep.BUY, locale, CocktailsBuyModel(listOf(CocktailsBuyItem("Ликёр", 7)))).shouldNotBeBlank()
        }
        render(CocktailsStep.MAIN, de, CocktailsMainModel(3, 1, 2, 3)) shouldContain "What to make"
    }

    should("ask to mark ingredients when nothing is selected") {
        render(CocktailsStep.MAIN, ru, CocktailsMainModel(0, 0, 0, 0)) shouldContain "/ingredients"
        render(CocktailsStep.MAIN, ru, CocktailsMainModel(0, 0, 0, 0)) shouldNotContain "Можно приготовить"
    }

    should("render a full card with escaped html, numbered steps and legend") {
        val text = render(CocktailsStep.CARD, ru, card)

        text shouldContain "<b>Мартини &lt;b&gt;</b>"
        text shouldContain "Автор: Автор"
        text shouldContain "✅ Водка — 50 мл\n❌ Ром — 20 мл"
        text shouldContain "✅ есть"
        text shouldContain "<b>Инвентарь</b>\nШейкер, Стрейнер"
        text shouldContain "1. Смешай\n2. Налей"
    }

    should("skip optional parts of a card") {
        val text = render(CocktailsStep.CARD, en, card.copy(author = null, tags = "", gadgets = "", steps = emptyList(), hasMarks = false))

        text shouldNotContain "Author"
        text shouldNotContain "Equipment"
        text shouldNotContain "Preparation"
        text shouldNotContain "✅ have"
    }

    should("list cocktails with missing ingredients and page info") {
        val text = render(
            CocktailsStep.LIST, ru,
            CocktailsListModel("Не хватает одного", 12, 2, 2, listOf(CocktailsListItem(9, "Алфа", "Ликёр"), CocktailsListItem(10, "Браво", ""))),
        )

        text shouldContain "Страница 2 из 2"
        text shouldContain "9. <b>Алфа</b> — не хватает: Ликёр\n10. <b>Браво</b>\n"
        text shouldNotContain "Ликёр\n\n10."
    }

    should("say when there is nothing to buy or list") {
        render(CocktailsStep.BUY, en, CocktailsBuyModel(emptyList())) shouldContain "Nothing to suggest"
        render(CocktailsStep.LIST, en, CocktailsListModel("T", 0, 1, 1, emptyList())) shouldContain "Nothing here yet"
    }
})
