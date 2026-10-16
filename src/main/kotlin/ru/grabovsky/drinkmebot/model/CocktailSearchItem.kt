package ru.grabovsky.drinkmebot.model

/** Коктейль из поиска администратора: [hasPhoto] - загружено ли для него фото. */
data class CocktailSearchItem(
    val id: Int,
    val name: String,
    val hasPhoto: Boolean,
)
