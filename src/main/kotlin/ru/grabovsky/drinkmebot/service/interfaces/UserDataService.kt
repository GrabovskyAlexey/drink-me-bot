package ru.grabovsky.drinkmebot.service.interfaces

import java.util.Locale

/** Данные пользователя: выгрузка и полное удаление (`/deleteme`). */
interface UserDataService {

    /** Все данные пользователя (профиль, настройки, отмеченные ингредиенты, избранные и приготовленные коктейли, дни активности) JSON-файлом; названия на языке [uiLocale]. */
    fun export(userId: Long, uiLocale: Locale): ByteArray

    /** Удаляет пользователя вместе с профилем, отмеченными ингредиентами, активностью и состоянием диалогов. */
    fun deleteAll(userId: Long)
}
