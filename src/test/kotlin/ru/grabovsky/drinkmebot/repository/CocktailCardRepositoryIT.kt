package ru.grabovsky.drinkmebot.repository

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.jdbc.datasource.init.ScriptUtils
import org.testcontainers.DockerClientFactory
import org.testcontainers.postgresql.PostgreSQLContainer
import ru.grabovsky.drinkmebot.model.CardIngredient

/** Проверяет SQL карточки коктейля на настоящем PostgreSQL. Без Docker тесты не создаются. */
class CocktailCardRepositoryIT : ShouldSpec({
    if (DockerClientFactory.instance().isDockerAvailable) defineTests()
})

private fun ShouldSpec.defineTests() {
    val postgres = PostgreSQLContainer("postgres:17")
    postgres.start()
    val dataSource = DriverManagerDataSource(postgres.jdbcUrl, postgres.username, postgres.password)
    val jdbc = JdbcTemplate(dataSource)
    val repository = CocktailCardRepository(NamedParameterJdbcTemplate(dataSource))
    afterSpec { postgres.stop() }

    fun setStock(userId: Long, vararg goodsIds: Int) {
        jdbc.update("DELETE FROM drink_me_bot.user_ingredient WHERE user_id = ?", userId)
        jdbc.update("INSERT INTO drink_me_bot.users (id) VALUES (?) ON CONFLICT DO NOTHING", userId)
        goodsIds.forEach { jdbc.update("INSERT INTO drink_me_bot.user_ingredient (user_id, goods_id) VALUES (?, ?)", userId, it) }
    }

    fun addGoods(id: Int, essential: Boolean, ru: String) {
        jdbc.update("INSERT INTO drink_me_bot.goods (id, is_essential) VALUES (?, ?)", id, essential)
        jdbc.update("INSERT INTO drink_me_bot.goods_i18n (goods_id, lang, name, description) VALUES (?, 'ru', ?, '')", id, ru)
    }

    fun addComposition(cocktailId: Int, kind: String, goodsId: Int, position: Int, name: String, amount: String, unit: String) {
        jdbc.update(
            "INSERT INTO drink_me_bot.cocktail_goods (cocktail_id, lang, kind, goods_id, position, name, amount, unit_code) " +
                "VALUES (?, 'ru', ?, ?, ?, ?, ?::numeric, ?)",
            cocktailId, kind, goodsId, position, name, amount, unit,
        )
    }

    beforeSpec {
        listOf(
            "liquibase/mvp/1.create_tables.sql",
            "liquibase/catalog/1.create_catalog_tables.sql",
            "liquibase/catalog/4.create_user_ingredient.sql",
            "liquibase/catalog/8.create_goods_substitute.sql",
        ).forEach { script -> dataSource.connection.use { ScriptUtils.executeSqlScript(it, ClassPathResource(script)) } }
        jdbc.update("INSERT INTO drink_me_bot.lang (code) VALUES ('ru')")
        // у единицы OZ нет подписи на ru - берётся код в нижнем регистре
        jdbc.update("INSERT INTO drink_me_bot.unit (code) VALUES ('ML'), ('OZ'), ('PIECE')")
        jdbc.update("INSERT INTO drink_me_bot.unit_i18n (unit_code, lang, label) VALUES ('ML', 'ru', 'мл'), ('PIECE', 'ru', 'шт')")

        addGoods(1, true, "Водка")
        addGoods(2, false, "Сок лайма")
        addGoods(3, false, "Сироп")
        addGoods(5, false, "Шейкер")
        addGoods(7, true, "Трипл сек")
        addGoods(8, true, "Драй Оранж")
        jdbc.update("INSERT INTO drink_me_bot.goods_substitute (goods_id, substitute_id) VALUES (7, 8), (8, 7)")

        jdbc.update("INSERT INTO drink_me_bot.cocktail (id) VALUES (100), (101)")
        jdbc.update("INSERT INTO drink_me_bot.cocktail_i18n (cocktail_id, lang, name, author) VALUES (100, 'ru', 'Алфа', 'Автор'), (101, 'ru', 'Браво', '')")
        jdbc.update("INSERT INTO drink_me_bot.cocktail_tag (cocktail_id, lang, position, tag) VALUES (100, 'ru', 2, 'кислые'), (100, 'ru', 1, 'крепкие')")
        jdbc.update("INSERT INTO drink_me_bot.cocktail_step (cocktail_id, lang, step_no, text) VALUES (100, 'ru', 2, 'Налей'), (100, 'ru', 1, 'Смешай')")
        addComposition(100, "ingredient", 2, 2, "Лайм", "12.50", "ML")
        addComposition(100, "ingredient", 1, 1, "Водка Царская", "50", "ML")
        addComposition(100, "ingredient", 3, 3, "Сироп", "0.5", "OZ")
        addComposition(100, "gadget", 5, 1, "Шейкер", "1", "PIECE")
        addComposition(101, "ingredient", 1, 1, "Водка", "30", "ML")
        jdbc.update("INSERT INTO drink_me_bot.cocktail (id) VALUES (102)")
        jdbc.update("INSERT INTO drink_me_bot.cocktail_i18n (cocktail_id, lang, name, author) VALUES (102, 'ru', 'Апельсиновый', '')")
        addComposition(102, "ingredient", 8, 1, "Драй Оранж", "20", "ML")
    }

    should("mark an ingredient as available through its substitute and name the substitute") {
        setStock(5L, 7)

        val ingredient = repository.find(5L, "ru", 102)!!.ingredients.single()

        ingredient.have shouldBe true
        ingredient.substitute shouldBe "Трипл сек"
    }

    should("not name a substitute when the ingredient itself is in stock or nothing is") {
        setStock(5L, 8, 7)
        repository.find(5L, "ru", 102)!!.ingredients.single().let { it.have to it.substitute } shouldBe (true to null)

        setStock(5L)
        repository.find(5L, "ru", 102)!!.ingredients.single().let { it.have to it.substitute } shouldBe (false to null)
    }

    should("build a card with ordered ingredients, tags, steps and gadgets") {
        setStock(1L, 1)

        val card = repository.find(1L, "ru", 100)!!

        card.name shouldBe "Алфа"
        card.author shouldBe "Автор"
        card.tags shouldBe listOf("крепкие", "кислые")
        card.steps shouldBe listOf("Смешай", "Налей")
        card.gadgets shouldBe listOf("Шейкер")
        card.ingredients shouldBe listOf(
            CardIngredient("Водка Царская", "50 мл", essential = true, have = true),
            CardIngredient("Лайм", "12.5 мл", essential = false, have = false),
            CardIngredient("Сироп", "0.5 oz", essential = false, have = false),
        )
    }

    should("mark an essential ingredient as missing when the user does not have it") {
        setStock(2L)

        repository.find(2L, "ru", 100)!!.ingredients.first().have shouldBe false
    }

    should("treat a blank author as absent") {
        repository.find(1L, "ru", 101)!!.author.shouldBeNull()
    }

    should("return nothing for an unknown cocktail or locale") {
        repository.find(1L, "ru", 999).shouldBeNull()
        repository.find(1L, "us", 100).shouldBeNull()
    }
}
