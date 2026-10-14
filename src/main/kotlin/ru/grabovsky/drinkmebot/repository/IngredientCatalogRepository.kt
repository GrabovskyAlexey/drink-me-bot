package ru.grabovsky.drinkmebot.repository

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import ru.grabovsky.drinkmebot.model.CategorySummary
import ru.grabovsky.drinkmebot.model.IngredientCategory
import ru.grabovsky.drinkmebot.model.IngredientItem

/** Справочник ингредиентов, которые пользователь может отметить (`goods_category`), с признаком «отмечен». */
@Repository
class IngredientCatalogRepository(
    private val jdbc: NamedParameterJdbcTemplate,
) {

    /** Размеры категорий и число отмеченных позиций; категории без записей в справочнике не возвращаются. */
    fun categorySummaries(userId: Long): List<CategorySummary> =
        jdbc.query(
            """
            SELECT gc.category, count(*) AS total, count(ui.goods_id) AS selected
            FROM drink_me_bot.goods_category gc
            LEFT JOIN drink_me_bot.user_ingredient ui ON ui.goods_id = gc.goods_id AND ui.user_id = :userId
            GROUP BY gc.category
            """.trimIndent(),
            MapSqlParameterSource("userId", userId),
        ) { rs, _ ->
            IngredientCategory.entries.firstOrNull { it.name == rs.getString("category") }
                ?.let { CategorySummary(it, rs.getInt("total"), rs.getInt("selected")) }
        }.filterNotNull().sortedBy { it.category.ordinal }

    /** Ингредиенты категории в локали [lang] по алфавиту; товары без перевода на эту локаль не показываются. */
    fun findByCategory(userId: Long, lang: String, category: IngredientCategory): List<IngredientItem> =
        jdbc.query(
            """
            $ITEMS_SELECT
            WHERE gc.category = :category
            ORDER BY gi.name, gi.goods_id
            """.trimIndent(),
            params(userId, lang).addValue("category", category.name),
        ) { rs, _ -> item(rs) }

    /** Поиск по части названия без учёта регистра (не больше [limit] позиций). */
    fun search(userId: Long, lang: String, query: String, limit: Int): List<IngredientItem> =
        jdbc.query(
            """
            $ITEMS_SELECT
            WHERE lower(gi.name) LIKE :pattern ESCAPE '\'
            ORDER BY gi.name, gi.goods_id
            LIMIT :limit
            """.trimIndent(),
            params(userId, lang).addValue("pattern", "%${escapeLike(query.lowercase())}%").addValue("limit", limit),
        ) { rs, _ -> item(rs) }

    /** Можно ли отметить этот товар (он есть в справочнике категорий). */
    fun isSelectable(goodsId: Int): Boolean =
        jdbc.queryForObject(
            "SELECT count(*) FROM drink_me_bot.goods_category WHERE goods_id = :goodsId",
            MapSqlParameterSource("goodsId", goodsId),
            Int::class.java,
        ) == 1

    private fun item(rs: java.sql.ResultSet) =
        IngredientItem(rs.getInt("goods_id"), rs.getString("name"), rs.getBoolean("selected"))

    private fun params(userId: Long, lang: String) =
        MapSqlParameterSource().addValue("userId", userId).addValue("lang", lang)

    private companion object {
        const val ITEMS_SELECT = """
            SELECT gi.goods_id, gi.name, (ui.goods_id IS NOT NULL) AS selected
            FROM drink_me_bot.goods_category gc
            JOIN drink_me_bot.goods_i18n gi ON gi.goods_id = gc.goods_id AND gi.lang = :lang
            LEFT JOIN drink_me_bot.user_ingredient ui ON ui.goods_id = gc.goods_id AND ui.user_id = :userId
        """

        fun escapeLike(value: String) = value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
    }
}
