package ru.grabovsky.drinkmebot.repository

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import ru.grabovsky.drinkmebot.model.CocktailMarks
import ru.grabovsky.drinkmebot.model.CocktailMatch

/**
 * Подборки коктейлей без подбора по ингредиентам: безалкогольные (нет алкогольных обязательных ингредиентов) и избранное
 * пользователя, а также отметки «избранное» / «уже готовил» (`user_cocktail`).
 */
@Repository
class CocktailCollectionRepository(
    private val jdbc: NamedParameterJdbcTemplate,
) {

    fun countNonAlcoholic(lang: String): Int =
        jdbc.queryForObject("WITH $COCKTAIL_LANG_CTE SELECT count(*) FROM cocktail_lang cl " +
                "JOIN drink_me_bot.cocktail_i18n ci ON ci.cocktail_id = cl.cocktail_id AND ci.lang = cl.lang WHERE $NON_ALCOHOLIC", params(0, lang), Int::class.java) ?: 0

    fun findNonAlcoholic(lang: String, limit: Int, offset: Int): List<CocktailMatch> =
        jdbc.query(
            "WITH $COCKTAIL_LANG_CTE SELECT ci.cocktail_id, ci.name, (ci.lang <> :lang) AS fallback " +
                "FROM cocktail_lang cl JOIN drink_me_bot.cocktail_i18n ci ON ci.cocktail_id = cl.cocktail_id AND ci.lang = cl.lang " +
                "WHERE $NON_ALCOHOLIC ORDER BY ci.name, ci.cocktail_id LIMIT :limit OFFSET :offset",
            params(0, lang).addValue("limit", limit).addValue("offset", offset),
        ) { rs, _ -> match(rs.getInt("cocktail_id"), rs.getString("name"), rs.getBoolean("fallback")) }

    fun countFavorites(userId: Long, lang: String): Int =
        jdbc.queryForObject(
            "WITH $COCKTAIL_LANG_CTE SELECT count(*) FROM drink_me_bot.user_cocktail uc " +
                "JOIN cocktail_lang cl ON cl.cocktail_id = uc.cocktail_id " +
                "JOIN drink_me_bot.cocktail_i18n ci ON ci.cocktail_id = uc.cocktail_id AND ci.lang = cl.lang " +
                "WHERE uc.user_id = :userId AND uc.favorite",
            params(userId, lang), Int::class.java,
        ) ?: 0

    fun findFavorites(userId: Long, lang: String, limit: Int, offset: Int): List<CocktailMatch> =
        jdbc.query(
            "WITH $COCKTAIL_LANG_CTE SELECT ci.cocktail_id, ci.name, (ci.lang <> :lang) AS fallback " +
                "FROM drink_me_bot.user_cocktail uc " +
                "JOIN cocktail_lang cl ON cl.cocktail_id = uc.cocktail_id " +
                "JOIN drink_me_bot.cocktail_i18n ci ON ci.cocktail_id = uc.cocktail_id AND ci.lang = cl.lang " +
                "WHERE uc.user_id = :userId AND uc.favorite " +
                "ORDER BY ci.name, ci.cocktail_id LIMIT :limit OFFSET :offset",
            params(userId, lang).addValue("limit", limit).addValue("offset", offset),
        ) { rs, _ -> match(rs.getInt("cocktail_id"), rs.getString("name"), rs.getBoolean("fallback")) }

    fun marks(userId: Long, cocktailId: Int): CocktailMarks =
        jdbc.query(
            "SELECT favorite, cooked FROM drink_me_bot.user_cocktail WHERE user_id = :userId AND cocktail_id = :id",
            MapSqlParameterSource("userId", userId).addValue("id", cocktailId),
        ) { rs, _ -> CocktailMarks(rs.getBoolean("favorite"), rs.getBoolean("cooked")) }.firstOrNull() ?: CocktailMarks()

    /** Переключает «избранное»; возвращает новое значение. */
    fun toggleFavorite(userId: Long, cocktailId: Int): Boolean = toggle("favorite", userId, cocktailId)

    /** Переключает «уже готовил»; возвращает новое значение. */
    fun toggleCooked(userId: Long, cocktailId: Int): Boolean = toggle("cooked", userId, cocktailId)

    private fun toggle(column: String, userId: Long, cocktailId: Int): Boolean =
        jdbc.queryForObject(
            "INSERT INTO drink_me_bot.user_cocktail (user_id, cocktail_id, $column) VALUES (:userId, :id, TRUE) " +
                "ON CONFLICT (user_id, cocktail_id) DO UPDATE SET $column = NOT drink_me_bot.user_cocktail.$column, updated_at = NOW() " +
                "RETURNING $column",
            MapSqlParameterSource("userId", userId).addValue("id", cocktailId), Boolean::class.java,
        ) ?: false

    private fun match(id: Int, name: String, fallback: Boolean) =
        CocktailMatch(id, name, matchedCount = 0, missingCount = 0, missingGoods = emptyList(), fallback = fallback)

    private fun params(userId: Long, lang: String) = MapSqlParameterSource().addValue("userId", userId).addValue("lang", lang)

    private companion object {
        /** Коктейль локали, у которого есть ингредиенты, но нет ни одного алкогольного обязательного (сиропы категории `SYRUP` не в счёт). */
        const val NON_ALCOHOLIC = """
            EXISTS (
                SELECT 1 FROM drink_me_bot.cocktail_goods cg
                WHERE cg.cocktail_id = ci.cocktail_id AND cg.lang = ci.lang AND cg.kind = 'ingredient'
            )
            AND NOT EXISTS (
                SELECT 1 FROM drink_me_bot.cocktail_goods cg
                JOIN drink_me_bot.goods g ON g.id = cg.goods_id AND g.is_essential
                LEFT JOIN drink_me_bot.goods_category gc ON gc.goods_id = g.id
                WHERE cg.cocktail_id = ci.cocktail_id AND cg.lang = ci.lang AND cg.kind = 'ingredient'
                  AND gc.category IS DISTINCT FROM 'SYRUP'
            )
        """
    }
}
