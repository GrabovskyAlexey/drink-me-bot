package ru.grabovsky.drinkmebot.repository

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.jdbc.datasource.init.ScriptUtils
import org.testcontainers.DockerClientFactory
import org.testcontainers.postgresql.PostgreSQLContainer
import ru.grabovsky.drinkmebot.model.CategorySummary
import ru.grabovsky.drinkmebot.model.IngredientCategory
import ru.grabovsky.drinkmebot.model.IngredientItem

/** Проверяет SQL выбора ингредиентов на настоящем PostgreSQL. Без Docker тесты не создаются. */
class IngredientCatalogRepositoryIT : ShouldSpec({
    if (DockerClientFactory.instance().isDockerAvailable) defineTests()
})

private fun ShouldSpec.defineTests() {
    val postgres = PostgreSQLContainer("postgres:17")
    postgres.start()
    val dataSource = DriverManagerDataSource(postgres.jdbcUrl, postgres.username, postgres.password)
    val jdbc = JdbcTemplate(dataSource)
    val repository = IngredientCatalogRepository(NamedParameterJdbcTemplate(dataSource))
    afterSpec { postgres.stop() }

    fun setStock(userId: Long, vararg goodsIds: Int) {
        jdbc.update("DELETE FROM drink_me_bot.user_ingredient WHERE user_id = ?", userId)
        jdbc.update("INSERT INTO drink_me_bot.users (id) VALUES (?) ON CONFLICT DO NOTHING", userId)
        goodsIds.forEach { jdbc.update("INSERT INTO drink_me_bot.user_ingredient (user_id, goods_id) VALUES (?, ?)", userId, it) }
    }

    beforeSpec {
        listOf(
            "liquibase/mvp/1.create_tables.sql",
            "liquibase/catalog/1.create_catalog_tables.sql",
            "liquibase/catalog/4.create_user_ingredient.sql",
            "liquibase/catalog/5.create_goods_category.sql",
        ).forEach { script -> dataSource.connection.use { ScriptUtils.executeSqlScript(it, ClassPathResource(script)) } }
        jdbc.update("INSERT INTO drink_me_bot.lang (code) VALUES ('ru'), ('us')")
        // 1 водка (VODKA), 2 и 3 ром (RUM; товар 3 есть только в ru), 4 сироп (необязательный, вне категорий)
        listOf(1 to ("Водка" to "Vodka"), 2 to ("Ром тёмный" to "Dark rum"), 3 to ("Ром белый" to null), 4 to ("Сироп" to "Syrup"))
            .forEach { (id, names) ->
                jdbc.update("INSERT INTO drink_me_bot.goods (id, is_essential) VALUES (?, ?)", id, id != 4)
                jdbc.update("INSERT INTO drink_me_bot.goods_i18n (goods_id, lang, name, description) VALUES (?, 'ru', ?, '')", id, names.first)
                names.second?.let {
                    jdbc.update("INSERT INTO drink_me_bot.goods_i18n (goods_id, lang, name, description) VALUES (?, 'us', ?, '')", id, it)
                }
            }
        jdbc.update("INSERT INTO drink_me_bot.goods_category (goods_id, category) VALUES (1, 'VODKA'), (2, 'RUM'), (3, 'RUM')")
    }

    should("summarize categories with the number of marked items") {
        setStock(1L, 1, 3)

        repository.categorySummaries(1L) shouldBe listOf(
            CategorySummary(IngredientCategory.VODKA, 1, 1),
            CategorySummary(IngredientCategory.RUM, 2, 1),
        )
    }

    should("list category items alphabetically with marks and skip goods without translation") {
        setStock(1L, 3)

        repository.findByCategory(1L, "ru", IngredientCategory.RUM) shouldBe listOf(
            IngredientItem(3, "Ром белый", true),
            IngredientItem(2, "Ром тёмный", false),
        )
        repository.findByCategory(1L, "us", IngredientCategory.RUM) shouldBe listOf(IngredientItem(2, "Dark rum", false))
    }

    should("keep marks of different users independent") {
        setStock(1L, 1)
        setStock(2L)

        repository.findByCategory(1L, "ru", IngredientCategory.VODKA) shouldBe listOf(IngredientItem(1, "Водка", true))
        repository.findByCategory(2L, "ru", IngredientCategory.VODKA) shouldBe listOf(IngredientItem(1, "Водка", false))
    }

    should("search by part of the name ignoring case") {
        setStock(1L, 2)

        repository.search(1L, "ru", "РОМ", 10) shouldBe listOf(
            IngredientItem(3, "Ром белый", false),
            IngredientItem(2, "Ром тёмный", true),
        )
        repository.search(1L, "ru", "вод", 10) shouldBe listOf(IngredientItem(1, "Водка", false))
    }

    should("not find goods outside of the categories and limit results") {
        repository.search(1L, "ru", "сироп", 10).shouldBeEmpty()
        repository.search(1L, "ru", "ром", 1) shouldBe listOf(IngredientItem(3, "Ром белый", false))
    }

    should("treat like wildcards in the query literally") {
        repository.search(1L, "ru", "%", 10).shouldBeEmpty()
        repository.search(1L, "ru", "_", 10).shouldBeEmpty()
    }

    should("tell which goods can be selected") {
        repository.isSelectable(1) shouldBe true
        repository.isSelectable(4) shouldBe false
        repository.isSelectable(999) shouldBe false
    }
}
