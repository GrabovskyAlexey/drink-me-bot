package ru.grabovsky.drinkmebot.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.grabovsky.drinkmebot.entity.UserIngredient
import ru.grabovsky.drinkmebot.entity.UserIngredientId
import ru.grabovsky.drinkmebot.model.CategorySummary
import ru.grabovsky.drinkmebot.model.IngredientCategory
import ru.grabovsky.drinkmebot.model.IngredientItem
import ru.grabovsky.drinkmebot.repository.IngredientCatalogRepository
import ru.grabovsky.drinkmebot.repository.UserIngredientRepository
import ru.grabovsky.drinkmebot.service.interfaces.IngredientService
import ru.grabovsky.drinkmebot.service.interfaces.IngredientService.Companion.MAX_QUERY_LENGTH
import ru.grabovsky.drinkmebot.service.interfaces.IngredientService.Companion.SEARCH_LIMIT

@Service
class IngredientServiceImpl(
    private val catalog: IngredientCatalogRepository,
    private val userIngredients: UserIngredientRepository,
) : IngredientService {

    override fun categories(userId: Long): List<CategorySummary> = catalog.categorySummaries(userId)

    override fun items(userId: Long, lang: String, category: IngredientCategory): List<IngredientItem> =
        catalog.findByCategory(userId, lang, category)

    override fun search(userId: Long, lang: String, query: String): List<IngredientItem> {
        val normalized = query.trim().take(MAX_QUERY_LENGTH)
        if (normalized.isEmpty()) return emptyList()
        return catalog.search(userId, lang, normalized, SEARCH_LIMIT)
    }

    @Transactional
    override fun toggle(userId: Long, goodsId: Int): Boolean? {
        if (!catalog.isSelectable(goodsId)) return null
        val id = UserIngredientId(userId, goodsId)
        if (userIngredients.existsById(id)) {
            userIngredients.deleteById(id)
            return false
        }
        userIngredients.save(UserIngredient(id))
        return true
    }

    @Transactional
    override fun reset(userId: Long): Int = userIngredients.deleteAllByUserId(userId)

    override fun selectedCount(userId: Long): Int = userIngredients.countByUserId(userId).toInt()
}
