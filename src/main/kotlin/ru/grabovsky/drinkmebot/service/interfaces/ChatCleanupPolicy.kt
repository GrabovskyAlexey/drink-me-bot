package ru.grabovsky.drinkmebot.service.interfaces

/** Нужно ли удалять служебные сообщения бота в чате пользователя (настройка `/settings`). */
fun interface ChatCleanupPolicy {
    fun enabled(chatId: Long): Boolean
}
