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

/** Выбранные пользователем теги; пустой фильтр ничего не отсекает. */
data class CocktailFilter(val tags: Set<CocktailTag> = emptySet()) {
    val isEmpty: Boolean get() = tags.isEmpty()

    companion object {
        val NONE = CocktailFilter()
    }
}
