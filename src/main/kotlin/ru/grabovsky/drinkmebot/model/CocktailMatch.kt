package ru.grabovsky.drinkmebot.model

/** Коктейль в результатах подбора: сколько обязательных ингредиентов есть и каких не хватает. */
data class CocktailMatch(
    val cocktailId: Int,
    val name: String,
    val matchedCount: Int,
    val missingCount: Int,
    val missingGoods: List<GoodsRef>,
    /** Коктейля нет на языке пользователя: название показано на запасном английском. */
    val fallback: Boolean = false,
)
