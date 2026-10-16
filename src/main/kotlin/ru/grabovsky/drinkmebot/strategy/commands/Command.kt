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
    COCKTAILS("cocktails", "🍸 Что приготовить", "🍸 What to make", 3),
    SHOPPING("shopping", "🛒 Список покупок", "🛒 Shopping list", 4),
    SETTINGS("settings", "⚙️ Настройки", "⚙️ Settings", 80),
    DELETE_ME("deleteme", "🗑 Удалить мои данные", "🗑 Delete my data", 85),
    ADMIN("admin", "📊 Статистика", "📊 Statistics", 90, hidden = true),
    HELP("help", "❓ Помощь", "❓ Help", 99);
}
