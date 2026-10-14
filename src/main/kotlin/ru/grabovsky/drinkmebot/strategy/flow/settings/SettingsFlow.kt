package ru.grabovsky.drinkmebot.strategy.flow.settings

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.grabovsky.drinkmebot.service.interfaces.DataLocaleService
import ru.grabovsky.drinkmebot.service.interfaces.I18nService
import ru.grabovsky.drinkmebot.service.interfaces.UserService
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.AnswerCallbackAction
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.DeleteMessageAction
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.EditMessageAction
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowCallbackPayload
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowContext
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowHandler
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowInlineButton
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowKey
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowKeys
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowMessage
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowParseMode
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowResult
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowStartContext
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowStep
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.SendMessageAction
import ru.grabovsky.drinkmebot.strategy.flow.core.support.buildMessage
import ru.grabovsky.drinkmebot.util.DataLocale
import ru.grabovsky.drinkmebot.util.LocaleUtils
import java.util.Locale

enum class SettingsStep(override val key: String) : FlowStep {
    MAIN("main"),
}

/** Состояния нет: значения читаются из профиля при каждой перерисовке. */
data class SettingsStateData(val unused: Boolean = false)

data class SettingsMainModel(val language: String)

/**
 * `/settings`: язык бота (флаги прямо на главном экране) и автоудаление служебных сообщений.
 * Язык задаёт сразу всё: интерфейс (русский для `ru`, английский для остальных) и язык коктейлей (8 локалей данных).
 * Callback-кнопки: `SL:<код>`, `AUTO`, `CLEAN`, `CLOSE`.
 */
@Component
class SettingsFlow(
    private val userService: UserService,
    private val dataLocale: DataLocaleService,
    private val i18n: I18nService,
) : FlowHandler<SettingsStateData> {

    override val key: FlowKey = FlowKeys.SETTINGS
    override val payloadType: Class<SettingsStateData> = SettingsStateData::class.java

    override fun start(context: FlowStartContext): FlowResult<SettingsStateData> =
        FlowResult(
            SettingsStep.MAIN.key, SettingsStateData(),
            listOf(SendMessageAction(MAIN_BINDING, mainMessage(context.user.id, context.locale))),
        )

    override fun onMessage(context: FlowContext<SettingsStateData>, message: Message): FlowResult<SettingsStateData>? = null

    override fun onCallback(
        context: FlowContext<SettingsStateData>,
        callbackQuery: CallbackQuery,
        data: String,
    ): FlowResult<SettingsStateData>? {
        val userId = context.user.id
        val state = context.state.payload
        val answer = AnswerCallbackAction(callbackQuery.id)
        val parts = data.split(':')
        var locale = context.locale
        when (parts[0]) {
            "SL" -> {
                val code = parts.getOrNull(1)?.takeIf { it in DataLocale.SUPPORTED } ?: return null
                dataLocale.choose(userId, code)
                // Язык интерфейса мог поменяться: перерисовываем сразу на новом
                locale = LocaleUtils.interfaceLocale(code)
            }
            "AUTO" -> {
                dataLocale.choose(userId, null)
                locale = LocaleUtils.resolve(userService.getUser(userId))
            }
            "CLEAN" -> {
                val user = userService.getUser(userId) ?: return null
                val profile = user.profile ?: return null
                profile.settings.cleanChat = !profile.settings.cleanChat
                userService.saveUser(user)
            }
            "CLOSE" -> return FlowResult(SettingsStep.MAIN.key, state, listOf(DeleteMessageAction(MAIN_BINDING), answer), completed = true)
            else -> return null
        }
        return FlowResult(SettingsStep.MAIN.key, state, listOf(EditMessageAction(MAIN_BINDING, mainMessage(userId, locale)), answer))
    }

    private fun mainMessage(userId: Long, locale: Locale): FlowMessage {
        val clean = userService.getUser(userId)?.profile?.settings?.cleanChat ?: true
        // Пока язык не выбран, отмечаем тот, что определён по языку Telegram
        val current = dataLocale.resolve(userId, locale)
        val flags = DataLocale.SUPPORTED_ORDERED.mapIndexed { index, code ->
            val flag = DataLocale.flag(code)
            button(if (code == current) "✅ $flag" else flag, "SL:$code", row = index / FLAGS_PER_ROW, col = index % FLAGS_PER_ROW)
        }
        // Вернуться к языку Telegram можно, только если язык выбран вручную
        val auto = listOfNotNull(
            button(i18n.i18n("buttons.settings.auto", locale), "AUTO", row = FLAG_ROWS).takeIf { dataLocale.chosen(userId) != null },
        )
        val buttons = flags + auto + listOf(
            button(i18n.i18n("buttons.settings.clean.${if (clean) "on" else "off"}", locale), "CLEAN", row = FLAG_ROWS + auto.size),
            button(i18n.i18n("buttons.common.close", locale), "CLOSE", row = FlowInlineButton.LAST_ROW),
        )
        val model = SettingsMainModel(i18n.i18n("settings.language.$current", locale))
        // Текст рендерим на актуальном языке: сразу после выбора язык в контексте ещё прежний
        return key.buildMessage(step = SettingsStep.MAIN, model = model, inlineButtons = buttons, parseMode = FlowParseMode.HTML)
            .copy(locale = locale)
    }

    private fun button(text: String, data: String, row: Int, col: Int = 0) =
        FlowInlineButton(text, FlowCallbackPayload(key.value, data), row = row, col = col)

    companion object {
        const val MAIN_BINDING = "main"
        const val FLAGS_PER_ROW = 4
        private const val FLAG_ROWS = 2
    }
}
