package ru.grabovsky.drinkmebot.model

/**
 * Категория ингредиентов (таблица `goods_category`); порядок объявления - порядок показа.
 * [alcohol] - есть ли в категории алкоголь (без него коктейль безалкогольный); [optional] - ингредиенты необязательные:
 * их можно отметить, но подбор учитывает их только в строгом режиме (`CocktailFilter.strict`).
 */
enum class IngredientCategory(val alcohol: Boolean = true, val optional: Boolean = false) {
    VODKA,
    WHISKY,
    RUM,
    GIN,
    TEQUILA,
    BRANDY,
    LIQUEUR,
    WINE,
    BITTER,
    SYRUP(alcohol = false),
    OTHER,
    JUICE(alcohol = false, optional = true),
    MIXER(alcohol = false, optional = true),
    FRUIT(alcohol = false, optional = true),
    PANTRY(alcohol = false, optional = true);

    /** Ключ подписи в `messages_*.properties`. */
    val messageKey: String get() = "ingredients.category.${name.lowercase()}"

    companion object {
        /** Имена категорий без алкоголя (для SQL). */
        val NON_ALCOHOL_NAMES: List<String> = entries.filter { !it.alcohol }.map { it.name }

        /** Имена категорий необязательных ингредиентов (для SQL). */
        val OPTIONAL_NAMES: List<String> = entries.filter { it.optional }.map { it.name }
    }
}
