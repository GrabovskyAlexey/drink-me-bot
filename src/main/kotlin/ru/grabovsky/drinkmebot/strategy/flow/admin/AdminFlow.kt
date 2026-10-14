package ru.grabovsky.drinkmebot.strategy.flow.admin

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.grabovsky.drinkmebot.model.STATS_ZONE
import ru.grabovsky.drinkmebot.model.StatsSnapshot
import ru.grabovsky.drinkmebot.service.interfaces.I18nService
import ru.grabovsky.drinkmebot.service.interfaces.StatsService
import ru.grabovsky.drinkmebot.service.interfaces.UserService
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.*
import ru.grabovsky.drinkmebot.strategy.flow.core.support.buildMessage
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class AdminStep(override val key: String) : FlowStep {
    MAIN("main"),
}

/** Состояния нет: статистика считается при каждом открытии. */
data class AdminState(var unused: Boolean = false)

/** Модель шаблона `admin/main`: числа уже готовы к показу, недели - моноширинной таблицей. */
data class AdminView(
    val generatedAt: String,
    val stats: StatsSnapshot,
    /** Среднее число отмеченных ингредиентов у пользователей, которые их отмечали. */
    val averageMarks: String,
    /** Доля пользователей с ингредиентами, у которых нет ни одного готового коктейля, в процентах. */
    val withoutReadyPercent: Int,
    val weekTable: String,
)

/**
 * `/admin`: статистика бота для администратора. Права проверяются при запуске и в каждом callback-е.
 * Одно сообщение: «Обновить» пересчитывает статистику, «Закрыть» удаляет сообщение.
 */
@Component
class AdminFlow(
    private val statsService: StatsService,
    private val userService: UserService,
    private val i18n: I18nService,
) : FlowHandler<AdminState> {

    override val key: FlowKey = FlowKeys.ADMIN
    override val payloadType: Class<AdminState> = AdminState::class.java

    override fun start(context: FlowStartContext): FlowResult<AdminState> {
        if (!isAdmin(context.user.id)) return FlowResult(AdminStep.MAIN.key, AdminState(), emptyList(), completed = true)
        return FlowResult(
            AdminStep.MAIN.key, AdminState(),
            listOf(SendMessageAction(MAIN_BINDING, mainMessage(context.locale))),
        )
    }

    override fun onMessage(context: FlowContext<AdminState>, message: Message): FlowResult<AdminState>? = null

    override fun onCallback(
        context: FlowContext<AdminState>,
        callbackQuery: CallbackQuery,
        data: String,
    ): FlowResult<AdminState>? {
        if (!isAdmin(callbackQuery.from.id)) return null
        val state = context.state.payload
        val answer = AnswerCallbackAction(callbackQuery.id)
        return when (data) {
            "REFRESH" -> FlowResult(
                AdminStep.MAIN.key, state, listOf(EditMessageAction(MAIN_BINDING, mainMessage(context.locale)), answer),
            )
            "CLOSE" -> FlowResult(
                AdminStep.MAIN.key, state, listOf(DeleteMessageAction(MAIN_BINDING), answer), completed = true,
            )
            else -> null
        }
    }

    private fun isAdmin(userId: Long) = userService.getUser(userId)?.profile?.isAdmin == true

    private fun mainMessage(locale: Locale, now: Instant = Instant.now()): FlowMessage {
        val stats = statsService.snapshot(now)
        val ingredients = stats.ingredients
        val withIngredients = ingredients.usersWithIngredients
        val view = AdminView(
            generatedAt = TIME.format(now.atZone(STATS_ZONE)),
            stats = stats,
            averageMarks = if (withIngredients == 0L) "0"
            else String.format(Locale.ROOT, "%.1f", ingredients.totalMarks.toDouble() / withIngredients),
            withoutReadyPercent = if (withIngredients == 0L) 0 else (ingredients.usersWithoutReady * 100 / withIngredients).toInt(),
            weekTable = weekTable(stats),
        )
        return key.buildMessage(
            step = AdminStep.MAIN,
            model = view,
            inlineButtons = listOf(
                button(i18n.i18n("buttons.admin.refresh", locale), "REFRESH", 0),
                button(i18n.i18n("buttons.common.close", locale), "CLOSE", FlowInlineButton.LAST_ROW),
            ),
            parseMode = FlowParseMode.HTML,
        )
    }

    private fun weekTable(stats: StatsSnapshot): String {
        val header = String.format(Locale.ROOT, "%-6s %5s %7s", "нед.", "новых", "активн.")
        val rows = stats.weeks.map { String.format(Locale.ROOT, "%-6s %5d %7d", WEEK.format(it.weekStart), it.newUsers, it.activeUsers) }
        return (listOf(header) + rows).joinToString("\n")
    }

    private fun button(text: String, data: String, row: Int) =
        FlowInlineButton(text, FlowCallbackPayload(key.value, data), row)

    private companion object {
        const val MAIN_BINDING = "main"
        val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
        val WEEK: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM")
    }
}
