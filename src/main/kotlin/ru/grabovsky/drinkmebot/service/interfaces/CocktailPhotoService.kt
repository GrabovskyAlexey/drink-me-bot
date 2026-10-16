package ru.grabovsky.drinkmebot.service.interfaces

import ru.grabovsky.drinkmebot.model.CocktailSearchItem

/** Фото коктейлей: администратор загружает их в `/admin`, карточка показывает фото, если оно есть. */
interface CocktailPhotoService {
    /** Telegram file_id фото коктейля или `null`, если фото нет. */
    fun fileId(cocktailId: Int): String?

    fun save(cocktailId: Int, fileId: String, uploadedBy: Long)

    /** Удаляет фото; `false`, если его не было. */
    fun delete(cocktailId: Int): Boolean

    fun count(): Int

    /** Русское название коктейля; `null`, если такого коктейля нет. */
    fun name(cocktailId: Int): String?

    /** Поиск коктейлей по части названия или по id (не больше [SEARCH_LIMIT]); пустой запрос ничего не находит. */
    fun search(query: String): List<CocktailSearchItem>

    companion object {
        const val SEARCH_LIMIT = 8
        const val MAX_QUERY_LENGTH = 50
    }
}
