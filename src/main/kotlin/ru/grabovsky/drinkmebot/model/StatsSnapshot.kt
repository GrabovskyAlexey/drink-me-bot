package ru.grabovsky.drinkmebot.model

import java.time.LocalDate
import java.time.ZoneId

/** Часовой пояс, по которому считаются дни и недели статистики. */
val STATS_ZONE: ZoneId = ZoneId.of("Europe/Moscow")

/** Пользователи: всего, новые и активные за сегодня / 7 / 30 дней (включая сегодняшний). */
data class AudienceStats(
    val total: Long,
    val new1: Long,
    val new7: Long,
    val new30: Long,
    val active1: Long,
    val active7: Long,
    val active30: Long,
)

/** Ингредиенты пользователей и результат подбора (по каталогу ru; считается на текущий момент). */
data class IngredientStats(
    val usersWithIngredients: Long,
    val totalMarks: Long,
    /** Пользователи с ингредиентами, у которых нет ни одного коктейля раздела «можно приготовить». */
    val usersWithoutReady: Long,
)

data class PopularItem(val name: String, val count: Long)

/** Неделя с понедельника [weekStart]; последняя в списке - текущая, ещё не закончившаяся. */
data class WeekStats(
    val weekStart: LocalDate,
    val newUsers: Long,
    val activeUsers: Long,
)

data class StatsSnapshot(
    val audience: AudienceStats,
    val ingredients: IngredientStats,
    val cocktailViews: Long,
    val popularIngredients: List<PopularItem>,
    val popularCocktails: List<PopularItem>,
    val weeks: List<WeekStats>,
)
