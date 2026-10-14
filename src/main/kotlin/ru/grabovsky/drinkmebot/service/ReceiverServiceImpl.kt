package ru.grabovsky.drinkmebot.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.groupadministration.LeaveChat
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberUpdated
import org.telegram.telegrambots.meta.api.objects.message.Message
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.grabovsky.drinkmebot.service.interfaces.FlowStateService
import ru.grabovsky.drinkmebot.service.interfaces.ReceiverService
import ru.grabovsky.drinkmebot.service.interfaces.UserService
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowCallbackPayload
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowEngine
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowKey
import ru.grabovsky.drinkmebot.util.LocaleUtils
import ru.grabovsky.drinkmebot.util.TelegramLogUtils
import java.util.*

/** Обрабатывает не-командные апдейты. Бот работает только в личных чатах, остальные апдейты игнорируются. */
@Service
class ReceiverServiceImpl(
    private val userService: UserService,
    private val objectMapper: ObjectMapper,
    private val flowEngine: FlowEngine,
    private val flowStateService: FlowStateService,
    private val telegramClient: TelegramClient,
) : ReceiverService {

    override fun execute(update: Update) {
        try {
            when {
                update.hasCallbackQuery() -> processCallback(update.callbackQuery)
                update.hasMessage() -> processMessage(update.message)
                update.hasMyChatMember() -> processMyChatMember(update.myChatMember)
            }
        } catch (error: Exception) {
            // Без этого ошибка в потоке long polling теряется, и состояние flow молча не сохраняется
            logger.error(error) { "Failed to process update ${update.updateId}" }
        }
    }

    private fun processMessage(message: Message) {
        if (!message.chat.isUserChat) {
            logger.debug { "Skip message ${message.messageId} in non-private chat ${message.chat.id}" }
            return
        }
        val user = message.from
        logger.debug { "Received: ${TelegramLogUtils.formatMessage(message)}" }
        userService.createOrUpdateUser(user)
        val flowState = flowStateService.findListFlow(user.id) ?: run {
            logger.debug { "Skip message ${message.messageId} from userId=${user.id}: no active flow" }
            return
        }
        val handled = flowEngine.onMessage(
            FlowKey(flowState.flowKey),
            user,
            resolveLocale(user),
            message
        )
        if (!handled) {
            logger.debug {
                "Active flow ${flowState.flowKey} ignored message ${message.messageId} from userId=${user.id}"
            }
        }
    }

    private fun processCallback(callbackQuery: CallbackQuery) {
        logger.debug { "Received: ${TelegramLogUtils.formatCallbackQuery(callbackQuery)}" }
        if (callbackQuery.message?.chat?.isUserChat == false) {
            logger.debug { "Skip callback ${callbackQuery.id} from non-private chat" }
            return
        }
        val user = callbackQuery.from
        userService.createOrUpdateUser(user)
        val payload = parseFlowPayload(callbackQuery.data)
        if (payload == null) {
            logger.warn { "Callback data has unexpected format: ${callbackQuery.data}" }
            return
        }
        val flowKey = FlowKey(payload.flow)
        val locale = resolveLocale(user)
        if (flowEngine.onCallback(flowKey, user, locale, callbackQuery, payload.data)) {
            return
        }
        if (!flowEngine.start(flowKey, user, locale, payload.data)) {
            logger.warn { "Flow ${flowKey.value} not found for callback ${callbackQuery.data}" }
            return
        }
        val handled = flowEngine.onCallback(flowKey, user, locale, callbackQuery, payload.data)
        if (!handled) {
            logger.warn {
                "Callback ${callbackQuery.id ?: "unknown"} not handled even after restart for flow ${flowKey.value}"
            }
        }
    }

    /** Личный чат: пользователь заблокировал бота (`kicked`) или разблокировал его - отражаем это в профиле. Группы и каналы: выходим. */
    private fun processMyChatMember(chatMemberUpdated: ChatMemberUpdated) {
        val chat = chatMemberUpdated.chat
        val status = chatMemberUpdated.newChatMember.status
        if (!chat.isUserChat) {
            if (status == "member" || status == "administrator") leaveChat(chat.id)
            return
        }
        userService.updateBlockedStatus(chat.id, status == "kicked")
        logger.info { "MyChatMember update for user ${chat.id} with status $status" }
    }

    /** Бота добавили в группу или канал: личный бот там не нужен, сразу выходим. */
    private fun leaveChat(chatId: Long) {
        logger.info { "Bot was added to non-private chat $chatId, leaving" }
        runCatching { telegramClient.execute(LeaveChat(chatId.toString())) }
            .onFailure { logger.warn { "Failed to leave chat $chatId: ${it.message}" } }
    }

    private fun parseFlowPayload(data: String): FlowCallbackPayload? =
        runCatching {
            objectMapper.readValue(data, FlowCallbackPayload::class.java)
        }.getOrNull() ?: runCatching {
            val node = objectMapper.readTree(data) ?: return@runCatching null
            val flow = node.get("flow")?.asText()
                ?: node.get("flowKey")?.asText()
            val payload = node.get("data")?.asText()
            if (flow.isNullOrBlank() || payload.isNullOrBlank()) {
                return@runCatching null
            }
            FlowCallbackPayload(flow, payload)
        }.getOrNull()

    private fun resolveLocale(user: User): Locale {
        val stored = userService.getUser(user.id)
        return LocaleUtils.resolve(stored)
    }

    companion object {
        val logger = KotlinLogging.logger {}
    }
}
