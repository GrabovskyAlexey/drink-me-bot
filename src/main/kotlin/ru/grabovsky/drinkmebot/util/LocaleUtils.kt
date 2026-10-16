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
     * Язык интерфейса для выбранного языка бота (код локали данных): свой перевод у `ru`, `ua` (украинский, `uk` в Telegram),
     * `de`, `es`, `fr`, `it`; для `us` и `uk` (британский английский) - английский.
     */
    fun interfaceLocale(dataLang: String): Locale = Locale.forLanguageTag(
        when (dataLang) {
            "ua" -> "uk"
            "ru", "de", "es", "fr", "it" -> dataLang
            else -> "en"
        },
    )

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
                "uk" -> Locale.forLanguageTag("en") // украинский интерфейс отключён
                else -> Locale.forLanguageTag(locale.language)
            }
        }
        return when {
            normalized.startsWith("ru") -> Locale.forLanguageTag("ru")
            else -> Locale.forLanguageTag(normalized.take(2))
        }
    }
}
