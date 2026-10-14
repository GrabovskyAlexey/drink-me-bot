package ru.grabovsky.drinkmebot.util

import ru.grabovsky.drinkmebot.entity.User
import java.util.*

object LocaleUtils {
    val defaultLocale: Locale = Locale.forLanguageTag("ru")

    /** Язык интерфейса пользователя: выбранный в `/settings` язык бота, иначе язык Telegram. */
    fun resolve(user: User?): Locale {
        user?.profile?.settings?.cocktailLang?.takeIf { it in DataLocale.SUPPORTED }?.let { return interfaceLocale(it) }
        return resolve(user?.profile?.locale ?: user?.language)
    }

    /**
     * Язык интерфейса для выбранного языка бота (код локали данных): русский для `ru`, английский для остальных.
     * Полный перевод интерфейса на остальные языки - в бэклоге.
     */
    fun interfaceLocale(dataLang: String): Locale = if (dataLang == "ru") Locale.forLanguageTag("ru") else Locale.forLanguageTag("en")

    private fun resolve(language: String?): Locale {
        if (language.isNullOrBlank()) {
            return defaultLocale
        }
        val normalized = language.replace('_', '-').lowercase()
        // try as full language tag first
        val locale = runCatching { Locale.forLanguageTag(normalized) }.getOrNull()
        if (locale != null && locale.language.isNotBlank()) {
            return when (locale.language) {
                "ru" -> Locale.forLanguageTag("ru")
                else -> Locale.forLanguageTag(locale.language)
            }
        }
        return when {
            normalized.startsWith("ru") -> Locale.forLanguageTag("ru")
            else -> Locale.forLanguageTag(normalized.take(2))
        }
    }
}
