package ru.grabovsky.drinkmebot.service.interfaces

import ru.grabovsky.drinkmebot.model.CocktailCard

interface CocktailCardService {
    /** Карточка коктейля в локали данных [lang] с отметками «есть / не хватает» по ингредиентам пользователя. */
    fun card(userId: Long, lang: String, cocktailId: Int): CocktailCard?
}
