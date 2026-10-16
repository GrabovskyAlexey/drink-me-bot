package ru.grabovsky.drinkmebot.bot

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.telegram.telegrambots.extensions.bots.commandbot.CommandLongPollingTelegramBot
import ru.grabovsky.drinkmebot.util.TelegramLogUtils
import org.telegram.telegrambots.longpolling.BotSession
import org.telegram.telegrambots.longpolling.starter.AfterBotRegistration
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.message.Message
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.grabovsky.drinkmebot.config.BotConfig
import ru.grabovsky.drinkmebot.service.interfaces.I18nService
import ru.grabovsky.drinkmebot.service.interfaces.ReceiverService
import ru.grabovsky.drinkmebot.service.interfaces.UnknownCommandService
import ru.grabovsky.drinkmebot.strategy.commands.AbstractCommand
import java.util.Locale


@Component
class Bot(
    private val config: BotConfig,
    private val client: TelegramClient,
    private val receiverService: ReceiverService,
    private val unknownCommandService: UnknownCommandService,
    i18n: I18nService,
    commands: List<AbstractCommand>
): SpringLongPollingBot, CommandLongPollingTelegramBot(client, true, { config.name }) {

    init {
        registerAll(*commands.toTypedArray())
        val listed = commands.filter { it.listed }.sortedBy { it.sortOrder }
        // Русское меню - по умолчанию; пользователям с языком из MENU_LANGUAGES Telegram показывает меню на их языке
        SetMyCommands(listed.map { BotCommand(it.commandIdentifier, it.description) }).also { client.execute(it) }
        MENU_LANGUAGES.forEach { language ->
            val locale = Locale.forLanguageTag(if (language == "uk") "en" else language) // украинский отключён
            SetMyCommands(listed.map { BotCommand(it.commandIdentifier, i18n.i18n("commands.${it.commandIdentifier}", locale)) })
                .also { it.languageCode = language }
                .also { client.execute(it) }
        }
    }
    override fun getBotToken() = config.token

    override fun getUpdatesConsumer() = this

    /** Бот работает только в личных чатах: команды из групп не выполняем, а передаём в [processNonCommandUpdate], где они игнорируются. */
    override fun filter(message: Message) = !message.chat.isUserChat

    override fun processNonCommandUpdate(update: Update) {
        logger.debug { "Received: ${TelegramLogUtils.formatUpdate(update)}" }
        receiverService.execute(update)
    }

    /** Команды, которых нет, не должны попадать в диалоги (например, стать названием места): отвечаем сами. */
    override fun processInvalidCommandUpdate(update: Update) {
        logger.debug { "Unknown command: ${TelegramLogUtils.formatUpdate(update)}" }
        unknownCommandService.reply(update.message)
    }

    @AfterBotRegistration
    fun afterRegistration(botSession: BotSession) {
        logger.info {"Registered bot running state is: ${botSession.isRunning}" }
    }

    companion object {
        /** Языки меню команд (коды Telegram), кроме русского - он по умолчанию. */
        val MENU_LANGUAGES = listOf("en", "uk", "de", "es", "fr", "it")
        val logger = KotlinLogging.logger {}
    }
}