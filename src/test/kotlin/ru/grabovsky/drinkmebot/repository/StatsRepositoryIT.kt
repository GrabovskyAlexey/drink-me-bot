package ru.grabovsky.drinkmebot.repository

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.jdbc.datasource.init.ScriptUtils
import org.testcontainers.DockerClientFactory
import org.testcontainers.postgresql.PostgreSQLContainer
import ru.grabovsky.drinkmebot.model.PopularItem
import java.time.LocalDate

/** Проверяет SQL статистики на настоящем PostgreSQL. Без Docker тесты не создаются. */
class StatsRepositoryIT : ShouldSpec({
    if (DockerClientFactory.instance().isDockerAvailable) defineTests()
})

private fun ShouldSpec.defineTests() {
    val postgres = PostgreSQLContainer("postgres:17")
    postgres.start()
    val dataSource = DriverManagerDataSource(postgres.jdbcUrl, postgres.username, postgres.password)
    val jdbc = JdbcTemplate(dataSource)
    val repository = StatsRepository(NamedParameterJdbcTemplate(dataSource))
    afterSpec { postgres.stop() }

    fun addGoods(id: Int, essential: Boolean, name: String) {
        jdbc.update("INSERT INTO drink_me_bot.goods (id, is_essential) VALUES (?, ?)", id, essential)
        jdbc.update("INSERT INTO drink_me_bot.goods_i18n (goods_id, lang, name, description) VALUES (?, 'ru', ?, '')", id, name)
    }

    fun addCocktail(id: Int, name: String, vararg goods: Int) {
        jdbc.update("INSERT INTO drink_me_bot.cocktail (id) VALUES (?)", id)
        jdbc.update("INSERT INTO drink_me_bot.cocktail_i18n (cocktail_id, lang, name) VALUES (?, 'ru', ?)", id, name)
        goods.forEachIndexed { index, goodsId ->
            jdbc.update(
                "INSERT INTO drink_me_bot.cocktail_goods (cocktail_id, lang, kind, goods_id, position, name, amount, unit_code) " +
                    "VALUES (?, 'ru', 'ingredient', ?, ?, 'x', 1, 'ML')",
                id, goodsId, index + 1,
            )
        }
    }

    fun addUser(id: Long, vararg goods: Int) {
        jdbc.update("INSERT INTO drink_me_bot.users (id) VALUES (?)", id)
        goods.forEach { jdbc.update("INSERT INTO drink_me_bot.user_ingredient (user_id, goods_id) VALUES (?, ?)", id, it) }
    }

    beforeSpec {
        listOf(
            "liquibase/mvp/1.create_tables.sql",
            "liquibase/catalog/1.create_catalog_tables.sql",
            "liquibase/catalog/4.create_user_ingredient.sql",
            "liquibase/catalog/5.create_goods_category.sql",
            "liquibase/stats/1.create_stats_tables.sql",
        ).forEach { script -> dataSource.connection.use { ScriptUtils.executeSqlScript(it, ClassPathResource(script)) } }

        jdbc.update("INSERT INTO drink_me_bot.lang (code) VALUES ('ru')")
        jdbc.update("INSERT INTO drink_me_bot.unit (code) VALUES ('ML')")
        // 1 водка, 2 ликёр (обязательные), 3 лайм (необязательный)
        addGoods(1, true, "Водка")
        addGoods(2, true, "Ликёр")
        addGoods(3, false, "Лайм")
        addGoods(4, true, "Сироп")
        jdbc.update("INSERT INTO drink_me_bot.goods_category (goods_id, category) VALUES (4, 'SYRUP')")
        addCocktail(10, "Коктейль А", 1, 3) // нужна водка
        addCocktail(11, "Коктейль Б", 1, 2) // нужны водка и ликёр
        addUser(100, 1, 2) // видит А и Б
        addUser(101, 2) // ничего не может приготовить
        addUser(102) // ингредиентов нет
        addCocktail(12, "Лимонад с сиропом", 3, 4) // безалкогольный: сироп не алкоголь
        addUser(103, 4) // только сироп: готового коктейля нет
    }

    should("count users with ingredients, marks and users without a ready cocktail") {
        val stats = repository.ingredients()
        stats.usersWithIngredients shouldBe 3
        stats.totalMarks shouldBe 4
        stats.usersWithoutReady shouldBe 2
    }

    should("rank popular ingredients by number of users") {
        repository.popularIngredients(10) shouldBe listOf(PopularItem("Ликёр", 2), PopularItem("Водка", 1), PopularItem("Сироп", 1))
    }

    should("count audience and activity by day") {
        repository.countUsers() shouldBe 4
        val today = LocalDate.now()
        repository.touchActivity(100, today)
        repository.touchActivity(100, today)
        repository.touchActivity(101, today.minusDays(10))
        repository.countActiveSince(today) shouldBe 1
        repository.countActiveSince(today.minusDays(29)) shouldBe 2
    }

    should("accumulate cocktail views and rank popular cocktails") {
        repository.addCocktailView(11)
        repository.addCocktailView(11)
        repository.addCocktailView(10)
        repository.countCocktailViews() shouldBe 3
        repository.popularCocktails(10) shouldBe listOf(PopularItem("Коктейль Б", 2), PopularItem("Коктейль А", 1))
    }
}
