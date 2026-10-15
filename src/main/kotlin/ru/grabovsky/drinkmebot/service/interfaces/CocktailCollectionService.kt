package ru.grabovsky.drinkmebot.service.interfaces

import ru.grabovsky.drinkmebot.model.CocktailMarks
import ru.grabovsky.drinkmebot.model.CocktailMatch
import kotlin.random.Random

/** Безалкогольные коктейли, избранное, отметки на коктейлях и случайный коктейль из готовых. Результат зависит от локали данных. */
interface CocktailCollectionService {
    fun nonAlcoholicCount(lang: String): Int

    fun nonAlcoholic(lang: String, page: Int, pageSize: Int): List<CocktailMatch>

    fun favoritesCount(userId: Long, lang: String): Int

    fun favorites(userId: Long, lang: String, page: Int, pageSize: Int): List<CocktailMatch>

    fun marks(userId: Long, cocktailId: Int): CocktailMarks

    /** Переключает «избранное»; возвращает новое значение. */
    fun toggleFavorite(userId: Long, cocktailId: Int): Boolean

    /** Переключает «уже готовил»; возвращает новое значение. */
    fun toggleCooked(userId: Long, cocktailId: Int): Boolean

    /** Случайный коктейль из раздела «можно приготовить»; `null`, если ничего не готово. */
    fun randomReady(userId: Long, lang: String, random: Random = Random.Default): CocktailMatch?
}
