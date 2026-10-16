package ru.grabovsky.drinkmebot.strategy.flow.admin

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.grabovsky.drinkmebot.model.CocktailSearchItem
import ru.grabovsky.drinkmebot.model.STATS_ZONE
import ru.grabovsky.drinkmebot.model.StatsSnapshot
import ru.grabovsky.drinkmebot.service.interfaces.CocktailPhotoService
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
    PHOTO_SEARCH("photo_search"),
    PHOTO_PICK("photo_pick"),
    PHOTO_UPLOAD("photo_upload"),
}

/** [cocktailId] - коктейль, для которого загружается фото (шаг `PHOTO_UPLOAD`); статистика состояния не требует. */
data class AdminState(var unused: Boolean = false, val cocktailId: Int? = null)

data class AdminPhotoSearchModel(val photos: Int)

data class AdminPhotoPickModel(val query: String, val items: List<CocktailSearchItem>)

/** [saved] - фото только что сохранено, [removed] - только что удалено. */
data class AdminPhotoUploadModel(val id: Int, val name: String, val hasPhoto: Boolean, val saved: Boolean = false, val removed: Boolean = false)

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
 * `/admin`: статистика бота для администратора. Права проверяются при запуске и в каждом сообщении и callback-е.
 * Одно сообщение: «Обновить» пересчитывает статистику, «Закрыть» удаляет сообщение.
 * Фото коктейлей: `PH` (поиск) -> текстом название или id -> `P:<id>` (выбор) -> фото сообщением; `PD` удаляет фото, `M` - назад.
 */
@Component
class AdminFlow(
    private val statsService: StatsService,
    private val photos: CocktailPhotoService,
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

    override fun onMessage(context: FlowContext<AdminState>, message: Message): FlowResult<AdminState>? {
        if (!isAdmin(context.user.id)) return null
        val state = context.state.payload
        return when (context.state.stepKey) {
            AdminStep.PHOTO_SEARCH.key, AdminStep.PHOTO_PICK.key -> {
                val query = message.text?.trim()?.takeIf { it.isNotEmpty() } ?: return null
                FlowResult(
                    AdminStep.PHOTO_PICK.key, state,
                    listOf(EditMessageAction(MAIN_BINDING, pickMessage(query)), DeleteMessageIdAction(message.messageId)),
                )
            }
            AdminStep.PHOTO_UPLOAD.key -> {
                val cocktailId = state.cocktailId ?: return null
                val fileId = message.photo?.maxByOrNull { it.fileSize ?: (it.width * it.height) }?.fileId ?: return null
                photos.save(cocktailId, fileId, context.user.id)
                FlowResult(
                    AdminStep.PHOTO_UPLOAD.key, state,
                    listOf(EditMessageAction(MAIN_BINDING, uploadMessage(cocktailId, saved = true) ?: return null), DeleteMessageIdAction(message.messageId)),
                )
            }
            else -> null
        }
    }

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
            "M" -> FlowResult(AdminStep.MAIN.key, state, listOf(EditMessageAction(MAIN_BINDING, mainMessage(context.locale)), answer))
            "PH" -> FlowResult(AdminStep.PHOTO_SEARCH.key, state, listOf(EditMessageAction(MAIN_BINDING, searchMessage()), answer))
            else -> photoCallback(context, data, answer)
        }
    }

    /** `P:<id>` - выбрать коктейль, `PD` - удалить его фото. */
    private fun photoCallback(context: FlowContext<AdminState>, data: String, answer: AnswerCallbackAction): FlowResult<AdminState>? {
        val parts = data.split(':')
        return when (parts[0]) {
            "P" -> {
                val id = parts.getOrNull(1)?.toIntOrNull() ?: return null
                val message = uploadMessage(id) ?: return null
                FlowResult(AdminStep.PHOTO_UPLOAD.key, AdminState(cocktailId = id), listOf(EditMessageAction(MAIN_BINDING, message), answer))
            }
            "PD" -> {
                val id = context.state.payload.cocktailId ?: return null
                photos.delete(id)
                val message = uploadMessage(id, removed = true) ?: return null
                FlowResult(AdminStep.PHOTO_UPLOAD.key, context.state.payload, listOf(EditMessageAction(MAIN_BINDING, message), answer))
            }
            else -> null
        }
    }

    private fun searchMessage(): FlowMessage =
        key.buildMessage(
            step = AdminStep.PHOTO_SEARCH, model = AdminPhotoSearchModel(photos.count()),
            inlineButtons = listOf(button(i18n.i18n("buttons.common.back", Locale.forLanguageTag("ru")), "M", FlowInlineButton.LAST_ROW)),
            parseMode = FlowParseMode.HTML,
        )

    private fun pickMessage(query: String): FlowMessage {
        val items = photos.search(query)
        val buttons = items.mapIndexed { index, item -> button("${if (item.hasPhoto) "🖼 " else ""}${item.name}", "P:${item.id}", index) } +
            button(i18n.i18n("buttons.common.back", Locale.forLanguageTag("ru")), "M", FlowInlineButton.LAST_ROW)
        return key.buildMessage(
            step = AdminStep.PHOTO_PICK, model = AdminPhotoPickModel(query.take(CocktailPhotoService.MAX_QUERY_LENGTH), items),
            inlineButtons = buttons, parseMode = FlowParseMode.HTML,
        )
    }

    /** Экран загрузки фото; `null`, если такого коктейля нет. */
    private fun uploadMessage(cocktailId: Int, saved: Boolean = false, removed: Boolean = false): FlowMessage? {
        val name = photos.name(cocktailId) ?: return null
        val hasPhoto = photos.fileId(cocktailId) != null
        val ru = Locale.forLanguageTag("ru")
        val buttons = listOfNotNull(
            button(i18n.i18n("buttons.admin.photo.delete", ru), "PD", 0).takeIf { hasPhoto },
            button(i18n.i18n("buttons.admin.photo.search", ru), "PH", 1),
            button(i18n.i18n("buttons.common.back", ru), "M", FlowInlineButton.LAST_ROW),
        )
        return key.buildMessage(
            step = AdminStep.PHOTO_UPLOAD, model = AdminPhotoUploadModel(cocktailId, name, hasPhoto, saved, removed),
            inlineButtons = buttons, parseMode = FlowParseMode.HTML,
        )
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
                button(i18n.i18n("buttons.admin.photos", locale), "PH", 1),
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
