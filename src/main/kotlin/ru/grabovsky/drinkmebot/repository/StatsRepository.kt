package ru.grabovsky.drinkmebot.repository

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import ru.grabovsky.drinkmebot.model.IngredientStats
import ru.grabovsky.drinkmebot.model.PopularItem
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate

/**
 * Статистика для `/admin` и счётчики активности. Названия берутся из каталога [STATS_LANG]; пользователи считаются
 * по `users`, активность - по `user_activity_day`.
 */
@Repository
class StatsRepository(
    private val jdbc: NamedParameterJdbcTemplate,
) {

    fun countUsers(): Long = count("SELECT count(*) FROM drink_me_bot.users")

    fun countUsersCreatedSince(since: Instant): Long =
        count("SELECT count(*) FROM drink_me_bot.users WHERE created_at >= :since", "since" to Timestamp.from(since))

    fun countActiveSince(day: LocalDate): Long =
        count("SELECT count(DISTINCT user_id) FROM drink_me_bot.user_activity_day WHERE day >= :day", "day" to day)

    /** Новые пользователи по неделям (ключ - понедельник по МСК) с момента [from]. */
    fun newUsersByWeek(from: Instant): Map<LocalDate, Long> =
        weekly(
            "SELECT date_trunc('week', created_at AT TIME ZONE 'Europe/Moscow')::date AS week, count(*) AS cnt " +
                "FROM drink_me_bot.users WHERE created_at >= :from GROUP BY week",
            "from" to Timestamp.from(from),
        )

    /** Активные пользователи по неделям (ключ - понедельник) с понедельника [from]. */
    fun activeUsersByWeek(from: LocalDate): Map<LocalDate, Long> =
        weekly(
            "SELECT date_trunc('week', day)::date AS week, count(DISTINCT user_id) AS cnt " +
                "FROM drink_me_bot.user_activity_day WHERE day >= :from GROUP BY week",
            "from" to from,
        )

    fun ingredients(): IngredientStats {
        val usersWithIngredients = count("SELECT count(DISTINCT user_id) FROM drink_me_bot.user_ingredient")
        val totalMarks = count("SELECT count(*) FROM drink_me_bot.user_ingredient")
        val withoutReady = count(
            """
            SELECT count(*)
            FROM (SELECT DISTINCT user_id FROM drink_me_bot.user_ingredient) u
            WHERE NOT EXISTS (
                SELECT 1
                FROM drink_me_bot.cocktail_goods cg
                JOIN drink_me_bot.goods g ON g.id = cg.goods_id AND g.is_essential
                LEFT JOIN drink_me_bot.goods_category gc ON gc.goods_id = g.id
                WHERE cg.lang = :lang AND cg.kind = 'ingredient'
                GROUP BY cg.cocktail_id
                HAVING bool_or(gc.category IS DISTINCT FROM 'SYRUP') AND bool_and(EXISTS (
                    SELECT 1 FROM drink_me_bot.user_ingredient ui WHERE ui.user_id = u.user_id AND ui.goods_id = cg.goods_id
                ))
            )
            """.trimIndent(),
            "lang" to STATS_LANG,
        )
        return IngredientStats(usersWithIngredients, totalMarks, withoutReady)
    }

    fun popularIngredients(limit: Int): List<PopularItem> =
        popular(
            """
            SELECT gi.name, count(*) AS cnt
            FROM drink_me_bot.user_ingredient ui
            JOIN drink_me_bot.goods_i18n gi ON gi.goods_id = ui.goods_id AND gi.lang = :lang
            GROUP BY gi.name
            ORDER BY cnt DESC, gi.name
            LIMIT :limit
            """.trimIndent(),
            limit,
        )

    fun popularCocktails(limit: Int): List<PopularItem> =
        popular(
            """
            SELECT ci.name, cv.views AS cnt
            FROM drink_me_bot.cocktail_view cv
            JOIN drink_me_bot.cocktail_i18n ci ON ci.cocktail_id = cv.cocktail_id AND ci.lang = :lang
            ORDER BY cv.views DESC, ci.name
            LIMIT :limit
            """.trimIndent(),
            limit,
        )

    fun countCocktailViews(): Long = count("SELECT coalesce(sum(views), 0) FROM drink_me_bot.cocktail_view")

    /** Отмечает день активности пользователя; повторная отметка за тот же день ничего не меняет. */
    fun touchActivity(userId: Long, day: LocalDate) {
        jdbc.update(
            "INSERT INTO drink_me_bot.user_activity_day (user_id, day) VALUES (:userId, :day) ON CONFLICT DO NOTHING",
            MapSqlParameterSource("userId", userId).addValue("day", day),
        )
    }

    fun addCocktailView(cocktailId: Int) {
        jdbc.update(
            "INSERT INTO drink_me_bot.cocktail_view (cocktail_id, views) VALUES (:id, 1) " +
                "ON CONFLICT (cocktail_id) DO UPDATE SET views = drink_me_bot.cocktail_view.views + 1",
            MapSqlParameterSource("id", cocktailId),
        )
    }

    private fun count(sql: String, vararg params: Pair<String, Any>): Long =
        jdbc.queryForObject(sql, MapSqlParameterSource(params.toMap()), Long::class.java) ?: 0L

    private fun weekly(sql: String, vararg params: Pair<String, Any>): Map<LocalDate, Long> =
        jdbc.query(sql, MapSqlParameterSource(params.toMap())) { rs, _ ->
            rs.getDate("week").toLocalDate() to rs.getLong("cnt")
        }.toMap()

    private fun popular(sql: String, limit: Int): List<PopularItem> =
        jdbc.query(sql, MapSqlParameterSource("lang", STATS_LANG).addValue("limit", limit)) { rs, _ ->
            PopularItem(rs.getString("name"), rs.getLong("cnt"))
        }

    companion object {
        /** Язык каталога для названий в статистике и подсчёта подбора. */
        const val STATS_LANG = "ru"
    }
}
