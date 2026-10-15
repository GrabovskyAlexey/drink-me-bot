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
import ru.grabovsky.drinkmebot.model.CocktailMarks

/** Проверяет SQL подборок и отметок на настоящем PostgreSQL. Без Docker тесты не создаются. */
class CocktailCollectionRepositoryIT : ShouldSpec({
    if (DockerClientFactory.instance().isDockerAvailable) defineTests()
})

private fun ShouldSpec.defineTests() {
    val postgres = PostgreSQLContainer("postgres:17")
    postgres.start()
    val dataSource = DriverManagerDataSource(postgres.jdbcUrl, postgres.username, postgres.password)
    val jdbc = JdbcTemplate(dataSource)
    val repository = CocktailCollectionRepository(NamedParameterJdbcTemplate(dataSource))
    afterSpec { postgres.stop() }

    fun addGoods(id: Int, essential: Boolean, name: String) {
        jdbc.update("INSERT INTO drink_me_bot.goods (id, is_essential) VALUES (?, ?)", id, essential)
        jdbc.update("INSERT INTO drink_me_bot.goods_i18n (goods_id, lang, name, description) VALUES (?, 'ru', ?, '')", id, name)
    }

    fun addCocktail(id: Int, name: String, vararg goods: Pair<Int, String>) {
        jdbc.update("INSERT INTO drink_me_bot.cocktail (id) VALUES (?)", id)
        jdbc.update("INSERT INTO drink_me_bot.cocktail_i18n (cocktail_id, lang, name) VALUES (?, 'ru', ?)", id, name)
        goods.forEachIndexed { index, (goodsId, kind) ->
            jdbc.update(
                "INSERT INTO drink_me_bot.cocktail_goods (cocktail_id, lang, kind, goods_id, position, name, amount, unit_code) " +
                    "VALUES (?, 'ru', ?, ?, ?, 'x', 1, 'ML')",
                id, kind, goodsId, index + 1,
            )
        }
    }

    beforeSpec {
        listOf(
            "liquibase/mvp/1.create_tables.sql",
            "liquibase/catalog/1.create_catalog_tables.sql",
            "liquibase/catalog/5.create_goods_category.sql",
            "liquibase/collections/1.create_user_cocktail.sql",
        ).forEach { script -> dataSource.connection.use { ScriptUtils.executeSqlScript(it, ClassPathResource(script)) } }
        jdbc.update("INSERT INTO drink_me_bot.lang (code) VALUES ('ru')")
        jdbc.update("INSERT INTO drink_me_bot.unit (code) VALUES ('ML')")
        // 1 водка (обязательная), 2 лимонад, 3 лайм (необязательные), 4 шейкер (гаджет)
        addGoods(1, true, "Водка")
        addGoods(2, false, "Лимонад")
        addGoods(3, false, "Лайм")
        addGoods(4, false, "Шейкер")
        addGoods(5, true, "Сироп") // обязательный, но не алкоголь
        jdbc.update("INSERT INTO drink_me_bot.goods_category (goods_id, category) VALUES (5, 'SYRUP')")
        addCocktail(10, "Мохито без рома", 2 to "ingredient", 3 to "ingredient", 4 to "gadget")
        addCocktail(11, "Лимонад", 2 to "ingredient")
        addCocktail(12, "Отвёртка", 1 to "ingredient", 2 to "ingredient")
        addCocktail(14, "Лимонад с сиропом", 2 to "ingredient", 5 to "ingredient")
        addCocktail(13, "Пустой", 4 to "gadget") // без ингредиентов: не безалкогольный
        jdbc.update("INSERT INTO drink_me_bot.users (id) VALUES (7), (8)")
    }

    should("list only cocktails that have ingredients but no alcoholic essential ones (syrups do not count)") {
        repository.countNonAlcoholic("ru") shouldBe 3
        repository.findNonAlcoholic("ru", 10, 0).map { it.name } shouldContainExactly listOf("Лимонад", "Лимонад с сиропом", "Мохито без рома")
        repository.findNonAlcoholic("ru", 1, 1).map { it.name } shouldContainExactly listOf("Лимонад с сиропом")
        repository.countNonAlcoholic("de") shouldBe 0
    }

    should("fall back to english for non-alcoholic cocktails missing from the user language") {
        jdbc.update("INSERT INTO drink_me_bot.lang (code) VALUES ('us') ON CONFLICT DO NOTHING")
        jdbc.update("INSERT INTO drink_me_bot.cocktail (id) VALUES (15)")
        jdbc.update("INSERT INTO drink_me_bot.cocktail_i18n (cocktail_id, lang, name) VALUES (15, 'us', 'Lemonade')")
        jdbc.update("INSERT INTO drink_me_bot.goods_i18n (goods_id, lang, name, description) VALUES (2, 'us', 'Lemonade', '') ON CONFLICT DO NOTHING")
        jdbc.update(
            "INSERT INTO drink_me_bot.cocktail_goods (cocktail_id, lang, kind, goods_id, position, name, amount, unit_code) " +
                "VALUES (15, 'us', 'ingredient', 2, 1, 'Lemonade', 1, 'ML')",
        )

        repository.countNonAlcoholic("ru") shouldBe 4
        val fallback = repository.findNonAlcoholic("ru", 10, 0).single { it.cocktailId == 15 }
        fallback.name shouldBe "Lemonade"
        fallback.fallback shouldBe true
        repository.findNonAlcoholic("ru", 10, 0).filter { it.cocktailId != 15 }.none { it.fallback } shouldBe true
        repository.countNonAlcoholic("us") shouldBe 1

        jdbc.update("DELETE FROM drink_me_bot.cocktail_goods WHERE cocktail_id = 15")
        jdbc.update("DELETE FROM drink_me_bot.goods_i18n WHERE goods_id = 2 AND lang = 'us'")
        jdbc.update("DELETE FROM drink_me_bot.cocktail_i18n WHERE cocktail_id = 15")
        jdbc.update("DELETE FROM drink_me_bot.cocktail WHERE id = 15")
    }

    should("toggle favorite and cooked marks independently") {
        repository.marks(7, 10) shouldBe CocktailMarks()

        repository.toggleFavorite(7, 10) shouldBe true
        repository.marks(7, 10) shouldBe CocktailMarks(favorite = true, cooked = false)

        repository.toggleCooked(7, 10) shouldBe true
        repository.toggleFavorite(7, 10) shouldBe false
        repository.marks(7, 10) shouldBe CocktailMarks(favorite = false, cooked = true)
        repository.marks(8, 10) shouldBe CocktailMarks()
    }

    should("list and count favorites of the user only") {
        repository.toggleFavorite(7, 12)
        repository.toggleFavorite(7, 11)
        repository.toggleFavorite(8, 12)

        repository.countFavorites(7, "ru") shouldBe 2
        repository.findFavorites(7, "ru", 10, 0).map { it.name } shouldContainExactly listOf("Лимонад", "Отвёртка")
        repository.countFavorites(8, "ru") shouldBe 1
        repository.countFavorites(7, "de") shouldBe 0
    }
}
