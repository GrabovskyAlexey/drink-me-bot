package ru.grabovsky.drinkmebot.service.interfaces

import ru.grabovsky.drinkmebot.model.ShoppingItem

/** Список покупок: что докупить для коктейлей, с отметкой «куплено». */
interface ShoppingListService {
    fun items(userId: Long, lang: String): List<ShoppingItem>

    /** Добавляет товары, которых ещё нет в списке; возвращает число добавленных. */
    fun add(userId: Long, goodsIds: Collection<Int>): Int

    /** Какие из [goodsIds] уже в списке. */
    fun contained(userId: Long, goodsIds: Collection<Int>): Set<Int>

    /** Переключает «куплено»; `null` - такой позиции нет. */
    fun toggleBought(userId: Long, goodsId: Int): Boolean?

    /** Убирает купленное из списка и отмечает как имеющиеся ингредиенты; возвращает число перенесённых. */
    fun moveBoughtToIngredients(userId: Long): Int

    /** Очищает список, возвращает число удалённых позиций. */
    fun clear(userId: Long): Int
}
