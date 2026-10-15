package ru.grabovsky.drinkmebot.repository

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import ru.grabovsky.drinkmebot.model.BuySuggestion
import ru.grabovsky.drinkmebot.model.CocktailMatch
import ru.grabovsky.drinkmebot.model.GoodsRef

/**
 * Подбор коктейлей по ингредиентам пользователя (`user_ingredient`).
 * Учитываются только обязательные ингредиенты (`goods.is_essential`), ингредиент считается имеющимся и при наличии его замены (`goods_substitute`); в подбор попадают лишь коктейли хотя бы с одним
 * алкогольным обязательным ингредиентом (не из категории `SYRUP`), безалкогольные показываются в своём разделе. Гаджеты и необязательные ингредиенты на результат не влияют.
 */
@Repository
class CocktailSuggestRepository(
    private val jdbc: NamedParameterJdbcTemplate,
) {

    /** Страница коктейлей, которым не хватает ровно [missing] обязательных ингредиентов (у «почти готовых» есть хотя бы один). */
    fun findByMissing(userId: Long, lang: String, missing: Int, limit: Int, offset: Int): List<CocktailMatch> =
        jdbc.query(
            """
            $REQUIRED_CTE
            SELECT s.cocktail_id,
                   ci.name,
                   (s.lang <> :lang) AS fallback,
                   s.matched,
                   s.missing,
                   array_agg(r.goods_id ORDER BY r.position) FILTER (WHERE NOT r.have) AS missing_ids,
                   array_agg(gi.name ORDER BY r.position)    FILTER (WHERE NOT r.have) AS missing_names
            FROM stats s
            JOIN drink_me_bot.cocktail_i18n ci ON ci.cocktail_id = s.cocktail_id AND ci.lang = s.lang
            JOIN required r ON r.cocktail_id = s.cocktail_id
            JOIN drink_me_bot.goods_i18n gi ON gi.goods_id = r.goods_id AND gi.lang = s.lang
            WHERE s.alcoholic > 0 AND s.missing = :missing AND (s.missing = 0 OR s.matched >= 1)
            GROUP BY s.cocktail_id, ci.name, s.lang, s.matched, s.missing
            ORDER BY s.matched DESC, ci.name, s.cocktail_id
            LIMIT :limit OFFSET :offset
            """.trimIndent(),
            params(userId, lang).addValue("missing", missing).addValue("limit", limit).addValue("offset", offset),
        ) { rs, _ ->
            val ids = rs.getArray("missing_ids")?.let { (it.array as Array<*>).map { id -> (id as Number).toInt() } }.orEmpty()
            val names = rs.getArray("missing_names")?.let { (it.array as Array<*>).map { name -> name as String } }.orEmpty()
            CocktailMatch(
                cocktailId = rs.getInt("cocktail_id"),
                name = rs.getString("name"),
                matchedCount = rs.getInt("matched"),
                missingCount = rs.getInt("missing"),
                missingGoods = ids.zip(names) { id, name -> GoodsRef(id, name) },
                fallback = rs.getBoolean("fallback"),
            )
        }

    /** Сколько коктейлей в каждом разделе: ключ - число недостающих (0..[maxMissing]), пустые разделы дают 0. */
    fun countByMissing(userId: Long, lang: String, maxMissing: Int): Map<Int, Int> {
        val counts = jdbc.query(
            """
            $REQUIRED_CTE
            SELECT s.missing, count(*) AS cnt
            FROM stats s
            JOIN drink_me_bot.cocktail_i18n ci ON ci.cocktail_id = s.cocktail_id AND ci.lang = s.lang
            WHERE s.alcoholic > 0 AND s.missing <= :maxMissing AND (s.missing = 0 OR s.matched >= 1)
            GROUP BY s.missing
            """.trimIndent(),
            params(userId, lang).addValue("maxMissing", maxMissing),
        ) { rs, _ -> rs.getInt("missing") to rs.getInt("cnt") }.toMap()
        return (0..maxMissing).associateWith { counts[it] ?: 0 }
    }

    /** Ингредиенты, которых не хватает в «почти готовых» коктейлях (одного ингредиента), по числу открываемых коктейлей. */
    fun findBuySuggestions(userId: Long, lang: String, limit: Int): List<BuySuggestion> =
        jdbc.query(
            """
            $REQUIRED_CTE
            SELECT r.goods_id, gi.name, count(*) AS unlocks
            FROM stats s
            JOIN drink_me_bot.cocktail_i18n ci ON ci.cocktail_id = s.cocktail_id AND ci.lang = s.lang
            JOIN required r ON r.cocktail_id = s.cocktail_id AND NOT r.have
            JOIN drink_me_bot.goods_i18n gi ON gi.goods_id = r.goods_id AND gi.lang = :lang
            WHERE s.alcoholic > 0 AND s.missing = 1 AND s.matched >= 1
            GROUP BY r.goods_id, gi.name
            ORDER BY unlocks DESC, gi.name, r.goods_id
            LIMIT :limit
            """.trimIndent(),
            params(userId, lang).addValue("limit", limit),
        ) { rs, _ -> BuySuggestion(GoodsRef(rs.getInt("goods_id"), rs.getString("name")), rs.getInt("unlocks")) }

    private fun params(userId: Long, lang: String) =
        MapSqlParameterSource().addValue("userId", userId).addValue("lang", lang)

    private companion object {
        /** `required` - обязательные ингредиенты коктейлей локали и флаг наличия у пользователя, `stats` - итоги по коктейлю. */
        const val REQUIRED_CTE = """
            WITH $COCKTAIL_LANG_CTE,
            required AS (
                SELECT cg.cocktail_id, cl.lang, cg.goods_id, cg.position,
                       EXISTS (
                           SELECT 1 FROM drink_me_bot.user_ingredient ui
                           WHERE ui.user_id = :userId
                             AND (ui.goods_id = cg.goods_id
                                  OR EXISTS (SELECT 1 FROM drink_me_bot.goods_substitute gs
                                             WHERE gs.goods_id = cg.goods_id AND gs.substitute_id = ui.goods_id))
                       ) AS have,
                       (gc.category IS DISTINCT FROM 'SYRUP') AS alcoholic
                FROM cocktail_lang cl
                JOIN drink_me_bot.cocktail_goods cg ON cg.cocktail_id = cl.cocktail_id AND cg.lang = cl.lang AND cg.kind = 'ingredient'
                JOIN drink_me_bot.goods g ON g.id = cg.goods_id AND g.is_essential
                LEFT JOIN drink_me_bot.goods_category gc ON gc.goods_id = cg.goods_id
            ),
            stats AS (
                SELECT cocktail_id, lang,
                       count(*) FILTER (WHERE have)     AS matched,
                       count(*) FILTER (WHERE NOT have) AS missing,
                       count(*) FILTER (WHERE alcoholic) AS alcoholic
                FROM required
                GROUP BY cocktail_id, lang
            )
        """
    }
}
