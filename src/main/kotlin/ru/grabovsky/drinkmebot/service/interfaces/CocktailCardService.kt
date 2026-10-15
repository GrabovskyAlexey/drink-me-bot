package ru.grabovsky.drinkmebot.service.interfaces

import ru.grabovsky.drinkmebot.model.CocktailCard

interface CocktailCardService {
    /** Карточка коктейля в локали данных [lang] с отметками «есть / не хватает» по ингредиентам пользователя. */
    /** [strict] - строгий подбор: необязательные ингредиенты из категорий соков, фруктов и т. п. тоже получают отметку. */
    fun card(userId: Long, lang: String, cocktailId: Int, strict: Boolean = false): CocktailCard?
}
