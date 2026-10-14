package ru.grabovsky.drinkmebot.strategy.flow.admin

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import ru.grabovsky.drinkmebot.config.FreemarkerConfig
import ru.grabovsky.drinkmebot.entity.User
import ru.grabovsky.drinkmebot.entity.UserProfile
import ru.grabovsky.drinkmebot.model.AudienceStats
import ru.grabovsky.drinkmebot.model.IngredientStats
import ru.grabovsky.drinkmebot.model.PopularItem
import ru.grabovsky.drinkmebot.model.StatsSnapshot
import ru.grabovsky.drinkmebot.model.WeekStats
import ru.grabovsky.drinkmebot.service.interfaces.I18nService
import ru.grabovsky.drinkmebot.service.interfaces.StatsService
import ru.grabovsky.drinkmebot.service.interfaces.UserService
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowKeys
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowStartContext
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.SendMessageAction
import ru.grabovsky.drinkmebot.strategy.flow.core.templating.FlowTemplateRenderer
import java.time.LocalDate
import java.util.Locale
import org.telegram.telegrambots.meta.api.objects.User as TgUser

class AdminFlowTest : ShouldSpec({
    val snapshot = StatsSnapshot(
        audience = AudienceStats(10, 1, 3, 8, 2, 5, 9),
        ingredients = IngredientStats(usersWithIngredients = 4, totalMarks = 10, usersWithoutReady = 1),
        cocktailViews = 7,
        popularIngredients = listOf(PopularItem("Водка <b>", 4)),
        popularCocktails = listOf(PopularItem("Негрони", 3)),
        weeks = listOf(WeekStats(LocalDate.parse("2026-06-15"), 3, 5)),
    )
    val stats = mockk<StatsService> { every { snapshot(any()) } returns snapshot }
    val i18n = object : I18nService {
        override fun i18n(code: String, locale: Locale, default: String?, vararg args: Any?) = code
    }
    val ru = Locale.forLanguageTag("ru")

    fun users(isAdmin: Boolean) = mockk<UserService> {
        every { getUser(any()) } returns User(7L, "T", null, "t").apply {
            profile = UserProfile(user = this, isAdmin = isAdmin)
        }
    }

    fun context() = FlowStartContext(mockk<TgUser> { every { id } returns 7L }, ru, null)

    should("send statistics to an admin") {
        val result = AdminFlow(stats, users(isAdmin = true), i18n).start(context())
        result.completed shouldBe false
        result.actions shouldHaveSize 1
        val message = (result.actions.single() as SendMessageAction).message
        message.stepKey shouldBe "main"
        (message.model as AdminView).stats shouldBe snapshot
    }

    should("do nothing for a non-admin") {
        val result = AdminFlow(stats, users(isAdmin = false), i18n).start(context())
        result.completed shouldBe true
        result.actions.shouldBeEmpty()
    }

    should("render the statistics and escape names") {
        val renderer = FlowTemplateRenderer(FreemarkerConfig().freeMarkerConfigurer().also { it.afterPropertiesSet() })
        val view = AdminView("15.06.2026 12:00", snapshot, "2.5", 25, "нед.")
        val text = renderer.render(FlowKeys.ADMIN, "main", ru, view)
        text shouldContain "Всего: <b>10</b>"
        text shouldContain "1. Водка &lt;b&gt; — 4"
        text shouldContain "Открытий карточек: 7"
        text shouldContain "(25%)"
    }
})
