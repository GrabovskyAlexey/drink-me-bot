package ru.grabovsky.drinkmebot.strategy.commands

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.message.Message
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.grabovsky.drinkmebot.service.interfaces.UnknownCommandService
import ru.grabovsky.drinkmebot.service.interfaces.UserService
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowEngine
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowKeys

/** `/admin` скрыта из меню бота и для всех, кроме администраторов (`UserProfile.isAdmin`), неотличима от несуществующей команды. */
@Component
class AdminCommand(
    userService: UserService,
    flowEngine: FlowEngine,
    private val unknownCommandService: UnknownCommandService,
) : AbstractCommand(Command.ADMIN, FlowKeys.ADMIN, userService, flowEngine) {

    /** Не администратору отвечаем как на любую несуществующую команду, чтобы не выдать, что `/admin` есть. */
    override fun processMessage(telegramClient: TelegramClient, message: Message, arguments: Array<out String>) {
        if (userService.getUser(message.from.id)?.profile?.isAdmin != true) {
            logger.info { "Treat /admin from non-admin user ${message.from.id} as unknown command" }
            unknownCommandService.reply(message)
            return
        }
        super.processMessage(telegramClient, message, arguments)
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
    }
}
