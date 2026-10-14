package ru.grabovsky.drinkmebot.model

/** «Что купить»: ингредиент и число коктейлей, которые станут доступны с ним (не хватает только его). */
data class BuySuggestion(
    val goods: GoodsRef,
    val unlocksCount: Int,
)
