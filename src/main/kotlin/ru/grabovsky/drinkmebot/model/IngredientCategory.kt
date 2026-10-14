package ru.grabovsky.drinkmebot.model

/** Категория обязательных ингредиентов (таблица `goods_category`); порядок объявления - порядок показа. */
enum class IngredientCategory {
    VODKA,
    WHISKY,
    RUM,
    GIN,
    TEQUILA,
    BRANDY,
    LIQUEUR,
    WINE,
    BITTER,
    OTHER;

    /** Ключ подписи в `messages_*.properties`. */
    val messageKey: String get() = "ingredients.category.${name.lowercase()}"
}
