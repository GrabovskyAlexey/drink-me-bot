package ru.grabovsky.drinkmebot.model

/** Ингредиент для выбора пользователем: [selected] - отмечен ли он как имеющийся. */
data class IngredientItem(
    val id: Int,
    val name: String,
    val selected: Boolean,
)
