package ru.grabovsky.drinkmebot.repository

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.jdbc.datasource.init.ScriptUtils
import org.testcontainers.DockerClientFactory
import org.testcontainers.postgresql.PostgreSQLContainer
import ru.grabovsky.drinkmebot.model.CocktailSearchItem

/** Проверяет SQL фото коктейлей на настоящем PostgreSQL. Без Docker тесты не создаются. */
class CocktailPhotoRepositoryIT : ShouldSpec({
    if (DockerClientFactory.instance().isDockerAvailable) defineTests()
})

private fun ShouldSpec.defineTests() {
    val postgres = PostgreSQLContainer("postgres:17")
    postgres.start()
    val dataSource = DriverManagerDataSource(postgres.jdbcUrl, postgres.username, postgres.password)
    val jdbc = JdbcTemplate(dataSource)
    val repository = CocktailPhotoRepository(NamedParameterJdbcTemplate(dataSource))
    afterSpec { postgres.stop() }

    beforeSpec {
        listOf(
            "liquibase/mvp/1.create_tables.sql",
            "liquibase/catalog/1.create_catalog_tables.sql",
            "liquibase/photos/1.create_cocktail_photo.sql",
        ).forEach { script -> dataSource.connection.use { ScriptUtils.executeSqlScript(it, ClassPathResource(script)) } }
        jdbc.update("INSERT INTO drink_me_bot.lang (code) VALUES ('ru'), ('us')")
        jdbc.update("INSERT INTO drink_me_bot.cocktail (id) VALUES (10), (11), (12)")
        jdbc.update(
            "INSERT INTO drink_me_bot.cocktail_i18n (cocktail_id, lang, name) VALUES " +
                "(10, 'ru', 'Негрони'), (11, 'ru', 'Мартини 100%'), (12, 'ru', 'Мохито'), (10, 'us', 'Negroni')",
        )
    }

    beforeTest { jdbc.update("DELETE FROM drink_me_bot.cocktail_photo") }

    should("save a photo, replace it and delete it") {
        repository.fileId(10).shouldBeNull()

        repository.save(10, "A", 1L)
        repository.save(10, "B", 2L)

        repository.fileId(10) shouldBe "B"
        repository.count() shouldBe 1
        repository.delete(10) shouldBe true
        repository.delete(10) shouldBe false
        repository.fileId(10).shouldBeNull()
    }

    should("find cocktails by a part of the russian name or by id and mark those with a photo") {
        repository.save(10, "A", 1L)

        repository.search("мо", 8) shouldContainExactly listOf(CocktailSearchItem(12, "Мохито", false))
        repository.search("НЕГР", 8) shouldContainExactly listOf(CocktailSearchItem(10, "Негрони", true))
        repository.search("11", 8) shouldContainExactly listOf(CocktailSearchItem(11, "Мартини 100%", false))
    }

    should("treat like wildcards in the query literally and respect the limit") {
        repository.search("%", 8).map { it.id } shouldContainExactly listOf(11)
        repository.search("", 2).size shouldBe 2
    }

    should("return the russian name of a cocktail") {
        repository.name(10) shouldBe "Негрони"
        repository.name(99).shouldBeNull()
    }
}
