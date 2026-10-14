package ru.grabovsky.drinkmebot.strategy.commands

import io.github.oshai.kotlinlogging.KotlinLogging
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.chat.Chat
import org.telegram.telegrambots.meta.api.objects.message.Message
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.grabovsky.drinkmebot.service.interfaces.UserService
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowEngine
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowKey
import ru.grabovsky.drinkmebot.util.LocaleUtils

/** Команда бота. Бот работает только в личных чатах: апдейты из групп отбрасывает [ru.grabovsky.drinkmebot.bot.Bot]. */
abstract class AbstractCommand(
    command: Command,
    protected val flowKey: FlowKey,
    protected val userService: UserService,
    private val flowEngine: FlowEngine,
    val sortOrder: Int = command.order,
    /** Показывать ли команду в меню бота (`SetMyCommands`). */
    val listed: Boolean = !command.hidden,
) : BotCommand(command.command, command.text), BotCommands {

    /** Описание команды в меню для английского языка (основное - [getDescription] - русское). */
    val descriptionEn: String = command.textEn

    override fun prepare(user: User, chat: Chat, arguments: Array<out String>) {
        userService.createOrUpdateUser(user)
    }

    override fun processMessage(telegramClient: TelegramClient, message: Message, arguments: Array<out String>) {
        super.processMessage(telegramClient, message, arguments)
        if (userService.getUser(message.from.id)?.profile?.settings?.cleanChat != false) {
            deleteCommandMessage(telegramClient, message)
        }
    }

    override fun execute(
        telegramClient: TelegramClient,
        user: User,
        chat: Chat,
        arguments: Array<out String>,
    ) {
        logger.info { "Process flow ${flowKey.value} for user ${user.userName ?: user.firstName} with id ${user.id}" }
        runCatching {
            prepare(user, chat, arguments)
            val locale = LocaleUtils.resolve(userService.getUser(user.id))
            val (startKey, startArgs) = resolveStart(arguments)
            if (!flowEngine.start(startKey, user, locale, startArgs)) {
                logger.error { "Flow $startKey not found, command processing aborted" }
            }
        }.onFailure { error ->
            logger.warn { "Error process flow ${flowKey.value} for user ${user.userName ?: user.firstName} with id ${user.id} with error: $error, stacktrace: ${error.stackTrace}" }
        }
    }

    /** Сообщение с командой после обработки только засоряет чат; не получилось удалить - не страшно. */
    private fun deleteCommandMessage(telegramClient: TelegramClient, message: Message) {
        runCatching {
            telegramClient.execute(DeleteMessage.builder().chatId(message.chatId).messageId(message.messageId).build())
        }.onFailure { error ->
            logger.debug { "Command message ${message.messageId} not deleted: ${error.message}" }
        }
    }

    /** Какой flow и с какими аргументами запускать (по умолчанию — свой, без аргументов). */
    protected open fun resolveStart(arguments: Array<out String>): Pair<FlowKey, String?> = flowKey to null

    companion object {
        val logger = KotlinLogging.logger {}
    }
}
