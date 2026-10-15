package ru.grabovsky.drinkmebot.service.interfaces

import ru.grabovsky.drinkmebot.model.CocktailFilter
import ru.grabovsky.drinkmebot.model.CocktailTag

/** Фильтр коктейлей по тегам: выбор хранится в настройках пользователя и действует на все разделы `/cocktails`. */
interface CocktailFilterService {
    fun get(userId: Long): CocktailFilter

    /** Включает или выключает тег; возвращает новый фильтр. */
    fun toggle(userId: Long, tag: CocktailTag): CocktailFilter

    fun reset(userId: Long)
}
