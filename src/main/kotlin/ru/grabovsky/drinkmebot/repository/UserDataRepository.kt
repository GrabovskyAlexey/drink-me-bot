package ru.grabovsky.drinkmebot.repository

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.time.LocalDate

/** Данные пользователя, нужные для выгрузки (`/deleteme`): отмеченные ингредиенты с названиями и дни активности. */
@Repository
class UserDataRepository(
    private val jdbc: NamedParameterJdbcTemplate,
) {

    /** Названия отмеченных ингредиентов на языке [lang]; если перевода нет, берётся английский (`us`). */
    fun ingredientNames(userId: Long, lang: String): List<String> =
        jdbc.queryForList(
            """
            SELECT coalesce(gi.name, us.name) AS name
            FROM drink_me_bot.user_ingredient ui
            LEFT JOIN drink_me_bot.goods_i18n gi ON gi.goods_id = ui.goods_id AND gi.lang = :lang
            LEFT JOIN drink_me_bot.goods_i18n us ON us.goods_id = ui.goods_id AND us.lang = 'us'
            WHERE ui.user_id = :userId
            ORDER BY name
            """.trimIndent(),
            MapSqlParameterSource("userId", userId).addValue("lang", lang),
            String::class.java,
        )

    /** Названия товаров из списка покупок на языке [lang] (иначе английские) и отметка «куплено». */
    fun shoppingItems(userId: Long, lang: String): List<Map<String, Any>> =
        jdbc.query(
            """
            SELECT coalesce(gi.name, us.name) AS name, si.bought
            FROM drink_me_bot.shopping_item si
            LEFT JOIN drink_me_bot.goods_i18n gi ON gi.goods_id = si.goods_id AND gi.lang = :lang
            LEFT JOIN drink_me_bot.goods_i18n us ON us.goods_id = si.goods_id AND us.lang = 'us'
            WHERE si.user_id = :userId
            ORDER BY name
            """.trimIndent(),
            MapSqlParameterSource("userId", userId).addValue("lang", lang),
        ) { rs, _ -> linkedMapOf<String, Any>("name" to rs.getString("name"), "bought" to rs.getBoolean("bought")) }

    /** Названия коктейлей, отмеченных пользователем как избранные ([favorite]) или как «уже готовил»; на языке [lang], иначе английские. */
    fun markedCocktails(userId: Long, lang: String, favorite: Boolean): List<String> =
        jdbc.queryForList(
            """
            SELECT coalesce(ci.name, us.name) AS name
            FROM drink_me_bot.user_cocktail uc
            LEFT JOIN drink_me_bot.cocktail_i18n ci ON ci.cocktail_id = uc.cocktail_id AND ci.lang = :lang
            LEFT JOIN drink_me_bot.cocktail_i18n us ON us.cocktail_id = uc.cocktail_id AND us.lang = 'us'
            WHERE uc.user_id = :userId AND (CASE WHEN :favorite THEN uc.favorite ELSE uc.cooked END)
            ORDER BY name
            """.trimIndent(),
            MapSqlParameterSource("userId", userId).addValue("lang", lang).addValue("favorite", favorite),
            String::class.java,
        ).filterNotNull()

    fun activityDays(userId: Long): List<LocalDate> =
        jdbc.query(
            "SELECT day FROM drink_me_bot.user_activity_day WHERE user_id = :userId ORDER BY day",
            MapSqlParameterSource("userId", userId),
        ) { rs, _ -> rs.getDate("day").toLocalDate() }
}
