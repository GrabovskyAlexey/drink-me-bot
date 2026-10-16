package ru.grabovsky.drinkmebot.repository

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import ru.grabovsky.drinkmebot.model.ShoppingItem

/** Список покупок пользователя (`shopping_item`). */
@Repository
class ShoppingListRepository(
    private val jdbc: NamedParameterJdbcTemplate,
) {

    /** Позиции списка: сначала не купленные, затем по алфавиту; название на [lang], если перевода нет - английское. */
    fun items(userId: Long, lang: String): List<ShoppingItem> =
        jdbc.query(
            """
            SELECT si.goods_id, coalesce(gi.name, us.name) AS name, si.bought
            FROM drink_me_bot.shopping_item si
            LEFT JOIN drink_me_bot.goods_i18n gi ON gi.goods_id = si.goods_id AND gi.lang = :lang
            LEFT JOIN drink_me_bot.goods_i18n us ON us.goods_id = si.goods_id AND us.lang = 'us'
            WHERE si.user_id = :userId
            ORDER BY si.bought, name, si.goods_id
            """.trimIndent(),
            MapSqlParameterSource("userId", userId).addValue("lang", lang),
        ) { rs, _ -> ShoppingItem(rs.getInt("goods_id"), rs.getString("name"), rs.getBoolean("bought")) }

    /** Добавляет товары, которых ещё нет в списке; возвращает, сколько добавлено. */
    fun add(userId: Long, goodsIds: Collection<Int>): Int {
        if (goodsIds.isEmpty()) return 0
        return jdbc.update(
            """
            INSERT INTO drink_me_bot.shopping_item (user_id, goods_id)
            SELECT :userId, g.id FROM drink_me_bot.goods g WHERE g.id IN (:ids)
            ON CONFLICT DO NOTHING
            """.trimIndent(),
            MapSqlParameterSource("userId", userId).addValue("ids", goodsIds),
        )
    }

    /** Какие из [goodsIds] уже в списке. */
    fun contained(userId: Long, goodsIds: Collection<Int>): Set<Int> {
        if (goodsIds.isEmpty()) return emptySet()
        return jdbc.queryForList(
            "SELECT goods_id FROM drink_me_bot.shopping_item WHERE user_id = :userId AND goods_id IN (:ids)",
            MapSqlParameterSource("userId", userId).addValue("ids", goodsIds), Int::class.java,
        ).toSet()
    }

    /** Переключает «куплено»; `null` - товара нет в списке. */
    fun toggleBought(userId: Long, goodsId: Int): Boolean? =
        jdbc.queryForList(
            """
            UPDATE drink_me_bot.shopping_item SET bought = NOT bought
            WHERE user_id = :userId AND goods_id = :goodsId
            RETURNING bought
            """.trimIndent(),
            MapSqlParameterSource("userId", userId).addValue("goodsId", goodsId), Boolean::class.java,
        ).firstOrNull()

    /** Убирает купленное из списка и отмечает эти товары как имеющиеся; возвращает число перенесённых. */
    fun moveBoughtToIngredients(userId: Long): Int =
        jdbc.update(
            """
            WITH done AS (
                DELETE FROM drink_me_bot.shopping_item WHERE user_id = :userId AND bought RETURNING goods_id
            )
            INSERT INTO drink_me_bot.user_ingredient (user_id, goods_id)
            SELECT :userId, goods_id FROM done
            ON CONFLICT DO NOTHING
            """.trimIndent(),
            MapSqlParameterSource("userId", userId),
        )

    fun clear(userId: Long): Int =
        jdbc.update("DELETE FROM drink_me_bot.shopping_item WHERE user_id = :userId", MapSqlParameterSource("userId", userId))
}
