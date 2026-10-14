package ru.grabovsky.drinkmebot.service.interfaces

import java.util.Locale

/** Язык коктейлей пользователя (локаль данных каталога, см. `DataLocale`): выбранный в `/settings` или определённый по языку Telegram. */
interface DataLocaleService {
    /** Локаль данных для пользователя; [uiLocale] - язык интерфейса, по нему выбирается язык при режиме «авто». */
    fun resolve(userId: Long, uiLocale: Locale): String

    /** Язык, выбранный пользователем явно; `null` - режим «авто». */
    fun chosen(userId: Long): String?

    /** Запоминает выбор: код из `DataLocale.SUPPORTED` или `null` для режима «авто». Неизвестный код игнорируется. */
    fun choose(userId: Long, lang: String?)
}
