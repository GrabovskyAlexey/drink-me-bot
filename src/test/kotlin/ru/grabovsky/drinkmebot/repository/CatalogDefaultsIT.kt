package ru.grabovsky.drinkmebot.repository

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.jdbc.datasource.init.ScriptUtils
import org.testcontainers.DockerClientFactory
import org.testcontainers.postgresql.PostgreSQLContainer

/** Проверяет значения по умолчанию справочника на настоящем PostgreSQL. Без Docker тесты не создаются. */
class CatalogDefaultsIT : ShouldSpec({
    if (DockerClientFactory.instance().isDockerAvailable) defineTests()
})

private fun ShouldSpec.defineTests() {
    val postgres = PostgreSQLContainer("postgres:17")
    postgres.start()
    val dataSource = DriverManagerDataSource(postgres.jdbcUrl, postgres.username, postgres.password)
    val jdbc = JdbcTemplate(dataSource)
    afterSpec { postgres.stop() }

    beforeSpec {
        listOf(
            "liquibase/mvp/1.create_tables.sql",
            "liquibase/catalog/1.create_catalog_tables.sql",
            "liquibase/catalog/6.goods_not_essential_by_default.sql",
        ).forEach { script -> dataSource.connection.use { ScriptUtils.executeSqlScript(it, ClassPathResource(script)) } }
    }

    should("make a new goods item non-essential unless a migration says otherwise") {
        jdbc.update("INSERT INTO drink_me_bot.goods (id) VALUES (1)")
        jdbc.update("INSERT INTO drink_me_bot.goods (id, is_essential) VALUES (2, TRUE)")
        jdbc.queryForObject("SELECT is_essential FROM drink_me_bot.goods WHERE id = 1", Boolean::class.java) shouldBe false
        jdbc.queryForObject("SELECT is_essential FROM drink_me_bot.goods WHERE id = 2", Boolean::class.java) shouldBe true
    }
}
