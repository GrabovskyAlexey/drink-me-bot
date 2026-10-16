package ru.grabovsky.drinkmebot.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.grabovsky.drinkmebot.model.ShoppingItem
import ru.grabovsky.drinkmebot.repository.ShoppingListRepository
import ru.grabovsky.drinkmebot.service.interfaces.ShoppingListService

@Service
class ShoppingListServiceImpl(
    private val repository: ShoppingListRepository,
) : ShoppingListService {
    override fun items(userId: Long, lang: String): List<ShoppingItem> = repository.items(userId, lang)

    @Transactional
    override fun add(userId: Long, goodsIds: Collection<Int>): Int = repository.add(userId, goodsIds)

    override fun contained(userId: Long, goodsIds: Collection<Int>): Set<Int> = repository.contained(userId, goodsIds)

    @Transactional
    override fun toggleBought(userId: Long, goodsId: Int): Boolean? = repository.toggleBought(userId, goodsId)

    @Transactional
    override fun moveBoughtToIngredients(userId: Long): Int = repository.moveBoughtToIngredients(userId)

    @Transactional
    override fun clear(userId: Long): Int = repository.clear(userId)
}
