package ru.grabovsky.drinkmebot.model

/** Группа тегов-фильтров: внутри группы теги работают как «или», между группами - как «и». */
enum class CocktailTagGroup { STRENGTH, TASTE, FORMAT, BASE }

/**
 * Тег-фильтр коктейлей. [ruTag] - тег в русском каталоге: он есть у всех коктейлей, а `cocktail_id` общий для всех языков,
 * поэтому фильтр работает одинаково на любом языке показа.
 */
enum class CocktailTag(val ruTag: String, val group: CocktailTagGroup) {
    STRONG("крепкие", CocktailTagGroup.STRENGTH),
    LOW_ALCOHOL("слабоалкогольные", CocktailTagGroup.STRENGTH),
    SWEET("сладкие", CocktailTagGroup.TASTE),
    SOUR("кислые", CocktailTagGroup.TASTE),
    CITRUS("цитрусовые", CocktailTagGroup.TASTE),
    BERRY("ягодные", CocktailTagGroup.TASTE),
    TROPICAL("тропические", CocktailTagGroup.TASTE),
    CREAMY("сливочные", CocktailTagGroup.TASTE),
    SHOTS("шоты", CocktailTagGroup.FORMAT),
    LONGS("лонги", CocktailTagGroup.FORMAT),
    VODKA("на водке", CocktailTagGroup.BASE),
    RUM("на роме", CocktailTagGroup.BASE),
    GIN("на джине", CocktailTagGroup.BASE),
    WHISKEY("на виски", CocktailTagGroup.BASE),
    TEQUILA("на текиле", CocktailTagGroup.BASE),
    COGNAC("на коньяке", CocktailTagGroup.BASE),
}

/**
 * Выбор пользователя для подбора: теги (пустой набор ничего не отсекает) и [strict] - строгий подбор,
 * при котором необязательные ингредиенты из категорий соков, фруктов и т. п. тоже должны быть отмечены.
 */
data class CocktailFilter(val tags: Set<CocktailTag> = emptySet(), val strict: Boolean = false) {
    /** Не выбрано ни одного тега (на [strict] не смотрит). */
    val isEmpty: Boolean get() = tags.isEmpty()

    companion object {
        val NONE = CocktailFilter()
    }
}
