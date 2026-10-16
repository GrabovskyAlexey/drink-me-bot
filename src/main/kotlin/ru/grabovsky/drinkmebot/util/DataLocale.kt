package ru.grabovsky.drinkmebot.util

import java.util.Locale

/**
 * Локаль данных каталога коктейлей (колонка `lang`): `ru`, `ua`, `uk`, `us`, `de`, `es`, `fr`, `it`.
 * Не совпадает с языком Telegram: английский (`en`) - это `us`, украинский (`uk` в Telegram) - это `ua`;
 * `uk` в данных - английский (Великобритания), его пользователь выбирает вручную.
 */
object DataLocale {
    const val DEFAULT = "us"

    /** Порядок показа в `/settings`. Украинский (`ua`) временно отключён: данные и перевод остались, но выбрать его нельзя. */
    val SUPPORTED_ORDERED: List<String> = listOf("ru", "us", "uk", "de", "es", "fr", "it")

    val SUPPORTED: Set<String> = SUPPORTED_ORDERED.toSet()

    private val FLAGS = mapOf(
        "ru" to "🇷🇺", "ua" to "🇺🇦", "us" to "🇺🇸", "uk" to "🇬🇧",
        "de" to "🇩🇪", "es" to "🇪🇸", "fr" to "🇫🇷", "it" to "🇮🇹",
    )

    /** Флаг локали для кнопок выбора языка. */
    fun flag(code: String): String = FLAGS.getValue(code)

    /** Локаль данных по языку интерфейса/Telegram; неизвестные языки получают английский ([DEFAULT]). */
    fun of(locale: Locale): String = when (val language = locale.language.lowercase()) {
        "en" -> "us"
        "uk" -> DEFAULT // украинский отключён, см. SUPPORTED_ORDERED
        in SUPPORTED -> language
        else -> DEFAULT
    }
}
