package ru.grabovsky.drinkmebot.service

import org.springframework.stereotype.Service
import ru.grabovsky.drinkmebot.model.BuySuggestion
import ru.grabovsky.drinkmebot.model.CocktailMatch
import ru.grabovsky.drinkmebot.repository.CocktailSuggestRepository
import ru.grabovsky.drinkmebot.service.interfaces.CocktailSuggestService
import ru.grabovsky.drinkmebot.service.interfaces.CocktailSuggestService.Companion.MAX_MISSING

@Service
class CocktailSuggestServiceImpl(
    private val repository: CocktailSuggestRepository,
) : CocktailSuggestService {

    override fun findCocktails(userId: Long, lang: String, missing: Int, page: Int, pageSize: Int): List<CocktailMatch> {
        require(missing in 0..MAX_MISSING) { "missing must be in 0..$MAX_MISSING, got $missing" }
        require(page >= 0 && pageSize > 0) { "page must be >= 0 and pageSize > 0" }
        return repository.findByMissing(userId, lang, missing, pageSize, page * pageSize)
    }

    override fun counts(userId: Long, lang: String): Map<Int, Int> =
        repository.countByMissing(userId, lang, MAX_MISSING)

    override fun whatToBuy(userId: Long, lang: String, limit: Int): List<BuySuggestion> =
        repository.findBuySuggestions(userId, lang, limit)
}
