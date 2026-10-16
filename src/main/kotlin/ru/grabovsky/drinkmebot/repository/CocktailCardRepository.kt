package ru.grabovsky.drinkmebot.repository

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import ru.grabovsky.drinkmebot.model.CardGadget
import ru.grabovsky.drinkmebot.model.CardIngredient
import ru.grabovsky.drinkmebot.model.CocktailCard
import ru.grabovsky.drinkmebot.model.IngredientCategory
import java.math.BigDecimal

/** Данные карточки коктейля: название, теги, состав (с отметками пользователя), гаджеты и шаги. */
@Repository
class CocktailCardRepository(
    private val jdbc: NamedParameterJdbcTemplate,
) {

    /** Карточка коктейля в локали [lang] (если на ней нет - на английском, `fallback`); `null`, если коктейля нет и на английском. */
    fun find(userId: Long, lang: String, cocktailId: Int, strict: Boolean = false, portions: Int = 1): CocktailCard? {
        // Коктейля нет на языке пользователя - показываем запасной английский
        val shown = if (hasCocktail(cocktailId, lang)) lang else FALLBACK_LANG
        val params = MapSqlParameterSource().addValue("userId", userId).addValue("lang", shown).addValue("id", cocktailId)
            .addValue("strict", strict)

        val (name, author) = jdbc.query(
            "SELECT name, author FROM drink_me_bot.cocktail_i18n WHERE cocktail_id = :id AND lang = :lang",
            params,
        ) { rs, _ -> rs.getString("name") to rs.getString("author") }.firstOrNull() ?: return null

        val tags = jdbc.queryForList(
            "SELECT tag FROM drink_me_bot.cocktail_tag WHERE cocktail_id = :id AND lang = :lang ORDER BY position",
            params, String::class.java,
        )
        val steps = jdbc.queryForList(
            "SELECT text FROM drink_me_bot.cocktail_step WHERE cocktail_id = :id AND lang = :lang ORDER BY step_no",
            params, String::class.java,
        )

        val goods = jdbc.query(
            """
            SELECT cg.kind, cg.goods_id, cg.name, cg.amount,
                   COALESCE(u.label, lower(cg.unit_code)) AS unit,
                   (g.is_essential OR (:strict AND gc.category IN (${IngredientCategory.OPTIONAL_NAMES.joinToString { "'$it'" }}))) AS is_essential,
                   (ui.goods_id IS NOT NULL) AS have,
                   (SELECT gi.name
                    FROM drink_me_bot.goods_substitute gs
                             JOIN drink_me_bot.user_ingredient sui ON sui.goods_id = gs.substitute_id AND sui.user_id = :userId
                             JOIN drink_me_bot.goods_i18n gi ON gi.goods_id = gs.substitute_id AND gi.lang = cg.lang
                    WHERE gs.goods_id = cg.goods_id
                    ORDER BY gi.name
                    LIMIT 1) AS substitute_name
            FROM drink_me_bot.cocktail_goods cg
            JOIN drink_me_bot.goods g ON g.id = cg.goods_id
            LEFT JOIN drink_me_bot.goods_category gc ON gc.goods_id = cg.goods_id
            LEFT JOIN drink_me_bot.unit_i18n u ON u.unit_code = cg.unit_code AND u.lang = cg.lang
            LEFT JOIN drink_me_bot.user_ingredient ui ON ui.goods_id = cg.goods_id AND ui.user_id = :userId
            WHERE cg.cocktail_id = :id AND cg.lang = :lang
            ORDER BY cg.kind, cg.position
            """.trimIndent(),
            params,
        ) { rs, _ ->
            Row(
                rs.getString("kind"), rs.getInt("goods_id"), rs.getString("name"), quantity(rs.getBigDecimal("amount") * BigDecimal(portions), rs.getString("unit")),
                rs.getBoolean("is_essential"), rs.getBoolean("have"), rs.getString("substitute_name"),
            )
        }

        return CocktailCard(
            id = cocktailId,
            name = name,
            author = author?.takeIf { it.isNotBlank() },
            tags = tags,
            ingredients = goods.filter { it.kind == INGREDIENT }.map { CardIngredient(it.name, it.quantity, it.essential, it.have || it.substitute != null, substitute = it.substitute.takeIf { _ -> !it.have }, goodsId = it.goodsId) },
            gadgets = goods.filter { it.kind == GADGET }.map { CardGadget(it.name, it.have) },
            hasGadgetMarks = hasGadgetMarks(userId),
            steps = steps,
            fallback = shown != lang,
        )
    }

    private fun hasGadgetMarks(userId: Long): Boolean =
        jdbc.queryForObject(
            """
            SELECT EXISTS (SELECT 1 FROM drink_me_bot.user_ingredient ui
                           JOIN drink_me_bot.goods_category gc ON gc.goods_id = ui.goods_id AND gc.category = 'GADGET'
                           WHERE ui.user_id = :userId)
            """.trimIndent(),
            MapSqlParameterSource("userId", userId), Boolean::class.java,
        ) ?: false

    private fun hasCocktail(cocktailId: Int, lang: String): Boolean =
        jdbc.queryForObject(
            "SELECT EXISTS (SELECT 1 FROM drink_me_bot.cocktail_i18n WHERE cocktail_id = :id AND lang = :lang)",
            MapSqlParameterSource().addValue("id", cocktailId).addValue("lang", lang), Boolean::class.java,
        ) ?: false

    /** [have] - есть сам товар, [substitute] - название отмеченной пользователем замены (если есть). */
    private data class Row(val kind: String, val goodsId: Int, val name: String, val quantity: String, val essential: Boolean, val have: Boolean, val substitute: String?)

    private companion object {
        const val INGREDIENT = "ingredient"
        const val GADGET = "gadget"

        fun quantity(amount: BigDecimal, unit: String) = "${amount.stripTrailingZeros().toPlainString()} $unit"
    }
}
