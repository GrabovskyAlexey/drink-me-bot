package ru.grabovsky.drinkmebot.util

import java.util.Locale

/**
 * Локаль данных каталога коктейлей (колонка `lang`): `ru`, `ua`, `uk`, `us`, `de`, `es`, `fr`, `it`.
 * Не совпадает с языком Telegram: английский (`en`) - это `us`, украинский (`uk` в Telegram) - это `ua`;
 * `uk` в данных - английский (Великобритания), его пользователь выбирает вручную.
 */
object DataLocale {
    const val DEFAULT = "us"

    val SUPPORTED: Set<String> = setOf("ru", "ua", "uk", "us", "de", "es", "fr", "it")

    /** Локаль данных по языку интерфейса/Telegram; неизвестные языки получают английский ([DEFAULT]). */
    fun of(locale: Locale): String = when (val language = locale.language.lowercase()) {
        "en" -> "us"
        "uk" -> "ua"
        in SUPPORTED -> language
        else -> DEFAULT
    }
}
