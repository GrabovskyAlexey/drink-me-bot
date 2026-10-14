package ru.grabovsky.drinkmebot.model

/** Категория ингредиентов: сколько в ней позиций и сколько из них отметил пользователь. */
data class CategorySummary(
    val category: IngredientCategory,
    val total: Int,
    val selected: Int,
)
