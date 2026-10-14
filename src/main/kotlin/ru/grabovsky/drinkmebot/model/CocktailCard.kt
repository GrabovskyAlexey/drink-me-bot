package ru.grabovsky.drinkmebot.model

/** Ингредиент в составе коктейля: [quantity] - количество с единицей («50 мл»), [essential] - обязательный ли, [have] - отмечен ли у пользователя. */
data class CardIngredient(
    val name: String,
    val quantity: String,
    val essential: Boolean,
    val have: Boolean,
)

/** Карточка коктейля в локали данных: состав, гаджеты и шаги приготовления. */
data class CocktailCard(
    val id: Int,
    val name: String,
    val author: String?,
    val tags: List<String>,
    val ingredients: List<CardIngredient>,
    val gadgets: List<String>,
    val steps: List<String>,
)
