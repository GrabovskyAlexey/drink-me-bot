package ru.grabovsky.drinkmebot.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.core.spec.style.ShouldSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.telegram.telegrambots.meta.api.methods.groupadministration.LeaveChat
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.chat.Chat
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberUpdated
import org.telegram.telegrambots.meta.api.objects.message.Message
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.grabovsky.drinkmebot.service.interfaces.FlowStateService
import ru.grabovsky.drinkmebot.service.interfaces.UserService
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowEngine

class ReceiverServiceImplTest : ShouldSpec({
    val userService = mockk<UserService>(relaxed = true)
    val flowEngine = mockk<FlowEngine>(relaxed = true)
    val flowStateService = mockk<FlowStateService>(relaxed = true)
    val client = mockk<TelegramClient>(relaxed = true)
    val service = ReceiverServiceImpl(userService, ObjectMapper(), flowEngine, flowStateService, client)

    fun chat(chatId: Long, private: Boolean): Chat = mockk {
        every { id } returns chatId
        every { isUserChat } returns private
    }

    fun memberUpdate(chat: Chat, newStatus: String): Update {
        val member = mockk<ChatMember> { every { status } returns newStatus }
        val updated = mockk<ChatMemberUpdated> {
            every { this@mockk.chat } returns chat
            every { newChatMember } returns member
        }
        return mockk {
            every { hasCallbackQuery() } returns false
            every { hasMessage() } returns false
            every { hasMyChatMember() } returns true
            every { myChatMember } returns updated
        }
    }

    beforeTest { clearMocks(userService, flowEngine, flowStateService, client) }

    should("ignore messages from group chats") {
        val message = mockk<Message> {
            every { chat } returns chat(-100L, private = false)
            every { messageId } returns 1
        }
        val update = mockk<Update> {
            every { hasCallbackQuery() } returns false
            every { hasMessage() } returns true
            every { this@mockk.message } returns message
        }

        service.execute(update)

        verify(exactly = 0) { userService.createOrUpdateUser(any()) }
        verify(exactly = 0) { flowEngine.onMessage(any(), any(), any(), any()) }
    }

    should("leave a group when the bot is added to it") {
        service.execute(memberUpdate(chat(-100L, private = false), "member"))

        verify { client.execute(match<LeaveChat> { it.chatId == "-100" }) }
    }

    should("not leave a group when the bot is removed from it") {
        service.execute(memberUpdate(chat(-100L, private = false), "left"))

        verify(exactly = 0) { client.execute(any<LeaveChat>()) }
    }

    should("mark the user as blocked when they stop the bot") {
        service.execute(memberUpdate(chat(7L, private = true), "kicked"))

        verify { userService.updateBlockedStatus(7L, true) }
    }

    should("mark the user as active when they restart the bot") {
        service.execute(memberUpdate(chat(7L, private = true), "member"))

        verify { userService.updateBlockedStatus(7L, false) }
    }
})
