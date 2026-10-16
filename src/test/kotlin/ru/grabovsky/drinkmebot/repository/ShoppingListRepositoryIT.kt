package ru.grabovsky.drinkmebot.repository

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.jdbc.datasource.init.ScriptUtils
import org.testcontainers.DockerClientFactory
import org.testcontainers.postgresql.PostgreSQLContainer

/** Проверяет SQL списка покупок на настоящем PostgreSQL. Без Docker тесты не создаются. */
class ShoppingListRepositoryIT : ShouldSpec({
    if (DockerClientFactory.instance().isDockerAvailable) defineTests()
})

private fun ShouldSpec.defineTests() {
    val postgres = PostgreSQLContainer("postgres:17")
    postgres.start()
    val dataSource = DriverManagerDataSource(postgres.jdbcUrl, postgres.username, postgres.password)
    val jdbc = JdbcTemplate(dataSource)
    val repository = ShoppingListRepository(NamedParameterJdbcTemplate(dataSource))
    afterSpec { postgres.stop() }

    beforeSpec {
        listOf(
            "liquibase/mvp/1.create_tables.sql",
            "liquibase/catalog/1.create_catalog_tables.sql",
            "liquibase/catalog/4.create_user_ingredient.sql",
            "liquibase/shopping/1.create_shopping_item.sql",
        ).forEach { script -> dataSource.connection.use { ScriptUtils.executeSqlScript(it, ClassPathResource(script)) } }
        jdbc.update("INSERT INTO drink_me_bot.lang (code) VALUES ('ru'), ('us')")
        jdbc.update("INSERT INTO drink_me_bot.goods (id, is_essential) VALUES (1, true), (2, true), (3, true)")
        jdbc.update(
            "INSERT INTO drink_me_bot.goods_i18n (goods_id, lang, name, description) VALUES (1, 'ru', 'Ром', ''), (2, 'ru', 'Водка', ''), (3, 'us', 'Gin', '')",
        )
    }

    beforeTest {
        jdbc.update("DELETE FROM drink_me_bot.shopping_item")
        jdbc.update("DELETE FROM drink_me_bot.user_ingredient")
        jdbc.update("INSERT INTO drink_me_bot.users (id) VALUES (1) ON CONFLICT DO NOTHING")
    }

    should("add goods once, skip unknown ones and list not bought first with an english fallback name") {
        repository.add(1L, listOf(1, 2, 3, 999)) shouldBe 3
        repository.add(1L, listOf(1, 2)) shouldBe 0
        repository.toggleBought(1L, 1) shouldBe true

        repository.items(1L, "ru").map { it.name to it.bought } shouldContainExactly listOf("Gin" to false, "Водка" to false, "Ром" to true)
        repository.contained(1L, listOf(1, 3, 4)) shouldBe setOf(1, 3)
    }

    should("toggle bought back and ignore goods that are not in the list") {
        repository.add(1L, listOf(1))

        repository.toggleBought(1L, 1) shouldBe true
        repository.toggleBought(1L, 1) shouldBe false
        repository.toggleBought(1L, 2) shouldBe null
    }

    should("move bought goods to ingredients and keep the rest") {
        repository.add(1L, listOf(1, 2))
        repository.toggleBought(1L, 1)

        repository.moveBoughtToIngredients(1L) shouldBe 1

        repository.items(1L, "ru").map { it.goodsId } shouldContainExactly listOf(2)
        jdbc.queryForList("SELECT goods_id FROM drink_me_bot.user_ingredient WHERE user_id = 1", Int::class.java) shouldContainExactly listOf(1)
    }

    should("clear the list") {
        repository.add(1L, listOf(1, 2))

        repository.clear(1L) shouldBe 2
        repository.items(1L, "ru").size shouldBe 0
    }
}
