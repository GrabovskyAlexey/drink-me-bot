package ru.grabovsky.drinkmebot.service

import org.springframework.stereotype.Service
import ru.grabovsky.drinkmebot.model.CocktailMarks
import ru.grabovsky.drinkmebot.model.CocktailMatch
import ru.grabovsky.drinkmebot.repository.CocktailCollectionRepository
import ru.grabovsky.drinkmebot.repository.CocktailSuggestRepository
import ru.grabovsky.drinkmebot.service.interfaces.CocktailCollectionService
import kotlin.random.Random

@Service
class CocktailCollectionServiceImpl(
    private val collections: CocktailCollectionRepository,
    private val suggestions: CocktailSuggestRepository,
) : CocktailCollectionService {

    override fun nonAlcoholicCount(lang: String): Int = collections.countNonAlcoholic(lang)

    override fun nonAlcoholic(lang: String, page: Int, pageSize: Int): List<CocktailMatch> =
        collections.findNonAlcoholic(lang, pageSize, page * pageSize)

    override fun favoritesCount(userId: Long, lang: String): Int = collections.countFavorites(userId, lang)

    override fun favorites(userId: Long, lang: String, page: Int, pageSize: Int): List<CocktailMatch> =
        collections.findFavorites(userId, lang, pageSize, page * pageSize)

    override fun marks(userId: Long, cocktailId: Int): CocktailMarks = collections.marks(userId, cocktailId)

    override fun toggleFavorite(userId: Long, cocktailId: Int): Boolean = collections.toggleFavorite(userId, cocktailId)

    override fun toggleCooked(userId: Long, cocktailId: Int): Boolean = collections.toggleCooked(userId, cocktailId)

    override fun randomReady(userId: Long, lang: String, random: Random): CocktailMatch? {
        val ready = suggestions.countByMissing(userId, lang, 0)[0] ?: 0
        if (ready == 0) return null
        return suggestions.findByMissing(userId, lang, missing = 0, limit = 1, offset = random.nextInt(ready)).firstOrNull()
    }
}
