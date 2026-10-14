package ru.grabovsky.drinkmebot.strategy.commands

enum class Command(
    val command: String,
    val text: String,
    /** Описание для меню у пользователей с английским Telegram. */
    val textEn: String,
    val order: Int,
    /** Скрытая команда работает, но не показывается в меню бота. */
    val hidden: Boolean = false,
) {
    START("start", "🟢 Начать пользоваться ботом", "🟢 Start using the bot", 1),
    INGREDIENTS("ingredients", "🧪 Мои ингредиенты", "🧪 My ingredients", 2),
    HELP("help", "❓ Помощь", "❓ Help", 99);
}
