package ru.grabovsky.drinkmebot.service

import org.springframework.stereotype.Service
import ru.grabovsky.drinkmebot.model.CocktailCard
import ru.grabovsky.drinkmebot.repository.CocktailCardRepository
import ru.grabovsky.drinkmebot.service.interfaces.CocktailCardService

@Service
class CocktailCardServiceImpl(
    private val repository: CocktailCardRepository,
) : CocktailCardService {
    override fun card(userId: Long, lang: String, cocktailId: Int, strict: Boolean, portions: Int): CocktailCard? =
        repository.find(userId, lang, cocktailId, strict, portions)
}
