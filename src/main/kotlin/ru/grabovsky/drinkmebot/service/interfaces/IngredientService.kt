package ru.grabovsky.drinkmebot.service.interfaces

import ru.grabovsky.drinkmebot.model.CategorySummary
import ru.grabovsky.drinkmebot.model.IngredientCategory
import ru.grabovsky.drinkmebot.model.IngredientItem

/** Ингредиенты пользователя: что у него есть. Выбор запоминается и используется при подборе коктейлей. */
interface IngredientService {
    fun categories(userId: Long): List<CategorySummary>

    fun items(userId: Long, lang: String, category: IngredientCategory): List<IngredientItem>

    /** Поиск по названию; пустой запрос ничего не находит. */
    fun search(userId: Long, lang: String, query: String): List<IngredientItem>

    /** Переключает наличие ингредиента: `true` - теперь отмечен, `false` - снят, `null` - такой ингредиент выбрать нельзя. */
    fun toggle(userId: Long, goodsId: Int): Boolean?

    /** Снимает все отметки, возвращает их число. */
    fun reset(userId: Long): Int

    fun selectedCount(userId: Long): Int

    companion object {
        const val SEARCH_LIMIT = 30
        const val MAX_QUERY_LENGTH = 50
    }
}
