package ru.grabovsky.drinkmebot.repository

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import ru.grabovsky.drinkmebot.model.CocktailFilter

/**
 * Условие `AND (...)` для коктейлей, чей `cocktail_id` задаёт [idColumn]; пустая строка, если фильтр пуст.
 * Теги сверяются по русскому каталогу (`lang = 'ru'`): в нём есть все коктейли. Группы тегов связаны через «и», теги внутри группы - через «или».
 * Параметры добавляются в [params].
 */
fun CocktailFilter.whereClause(idColumn: String, params: MapSqlParameterSource): String =
    tags.groupBy { it.group }.entries.sortedBy { it.key }.joinToString("") { (group, groupTags) ->
        val name = "filter_${group.name.lowercase()}"
        params.addValue(name, groupTags.map { it.ruTag })
        " AND EXISTS (SELECT 1 FROM drink_me_bot.cocktail_tag ft WHERE ft.cocktail_id = $idColumn AND ft.lang = 'ru' AND ft.tag IN (:$name))"
    }

