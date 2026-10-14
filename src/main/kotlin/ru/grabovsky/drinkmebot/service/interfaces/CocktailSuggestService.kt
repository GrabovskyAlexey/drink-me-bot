package ru.grabovsky.drinkmebot.service.interfaces

import ru.grabovsky.drinkmebot.model.BuySuggestion
import ru.grabovsky.drinkmebot.model.CocktailMatch

/** Подбор коктейлей по ингредиентам пользователя. Результат зависит от локали данных (см. `DataLocale`). */
interface CocktailSuggestService {
    /** Страница коктейлей, которым не хватает ровно [missing] (0..[MAX_MISSING]) обязательных ингредиентов. */
    fun findCocktails(userId: Long, lang: String, missing: Int, page: Int = 0, pageSize: Int = PAGE_SIZE): List<CocktailMatch>

    /** Размеры разделов: ключ - число недостающих ингредиентов (0..[MAX_MISSING]). */
    fun counts(userId: Long, lang: String): Map<Int, Int>

    /** Какие ингредиенты стоит докупить: каждый открывает [BuySuggestion.unlocksCount] коктейлей. */
    fun whatToBuy(userId: Long, lang: String, limit: Int = BUY_LIMIT): List<BuySuggestion>

    companion object {
        const val MAX_MISSING = 2
        const val PAGE_SIZE = 8
        const val BUY_LIMIT = 5
    }
}
