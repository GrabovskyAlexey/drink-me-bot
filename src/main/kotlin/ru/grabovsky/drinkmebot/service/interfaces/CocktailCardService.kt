package ru.grabovsky.drinkmebot.service.interfaces

import ru.grabovsky.drinkmebot.model.CocktailCard

interface CocktailCardService {
    /** Карточка коктейля в локали данных [lang] с отметками «есть / не хватает» по ингредиентам пользователя. */
    /** [strict] - строгий подбор: необязательные ингредиенты из категорий соков, фруктов и т. п. тоже получают отметку. */
    /** [portions] - во сколько раз пересчитать количества ингредиентов (1, 2, 4). */
    fun card(userId: Long, lang: String, cocktailId: Int, strict: Boolean = false, portions: Int = 1): CocktailCard?
}
