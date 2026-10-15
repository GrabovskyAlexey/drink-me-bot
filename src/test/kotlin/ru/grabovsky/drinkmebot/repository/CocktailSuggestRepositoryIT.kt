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
import ru.grabovsky.drinkmebot.model.GoodsRef

/** Проверяет SQL подбора на настоящем PostgreSQL с маленьким набором данных. Без Docker тесты не создаются. */
class CocktailSuggestRepositoryIT : ShouldSpec({
    if (DockerClientFactory.instance().isDockerAvailable) defineTests()
})

private fun ShouldSpec.defineTests() {
    val postgres = PostgreSQLContainer("postgres:17")
    postgres.start()
    val dataSource = DriverManagerDataSource(postgres.jdbcUrl, postgres.username, postgres.password)
    val jdbc = JdbcTemplate(dataSource)
    val repository = CocktailSuggestRepository(NamedParameterJdbcTemplate(dataSource))
    afterSpec { postgres.stop() }

    // Товары: 1 водка, 3 ликёр, 4 ром (обязательные), 2 лайм (необязательный), 5 шейкер (гаджет)
    val vodka = GoodsRef(1, "Водка")
    val liqueur = GoodsRef(3, "Ликёр")
    val rum = GoodsRef(4, "Ром")

    fun addGoods(id: Int, essential: Boolean, ru: String?, us: String?) {
        jdbc.update("INSERT INTO drink_me_bot.goods (id, is_essential) VALUES (?, ?)", id, essential)
        mapOf("ru" to ru, "us" to us).forEach { (lang, name) ->
            if (name != null) {
                jdbc.update("INSERT INTO drink_me_bot.goods_i18n (goods_id, lang, name, description) VALUES (?, ?, ?, '')", id, lang, name)
            }
        }
    }

    /** Коктейль с составом: список пар (goods_id, kind); порядок задаёт position. */
    fun addCocktail(id: Int, names: Map<String, String>, goods: List<Pair<Int, String>>) {
        jdbc.update("INSERT INTO drink_me_bot.cocktail (id) VALUES (?)", id)
        names.forEach { (lang, name) ->
            jdbc.update("INSERT INTO drink_me_bot.cocktail_i18n (cocktail_id, lang, name) VALUES (?, ?, ?)", id, lang, name)
            goods.forEachIndexed { index, (goodsId, kind) ->
                jdbc.update(
                    "INSERT INTO drink_me_bot.cocktail_goods (cocktail_id, lang, kind, goods_id, position, name, amount, unit_code) " +
                        "VALUES (?, ?, ?, ?, ?, 'x', 1, 'ML')",
                    id, lang, kind, goodsId, index + 1,
                )
            }
        }
    }

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
        )
            .forEach { script -> dataSource.connection.use { ScriptUtils.executeSqlScript(it, ClassPathResource(script)) } }
        jdbc.update("INSERT INTO drink_me_bot.lang (code) VALUES ('ru'), ('us')")
        jdbc.update("INSERT INTO drink_me_bot.unit (code) VALUES ('ML')")

        addGoods(1, true, "Водка", "Vodka")
        addGoods(2, false, "Сок лайма", "Lime juice")
        addGoods(3, true, "Ликёр", "Liqueur")
        addGoods(4, true, "Ром", "Rum")
        addGoods(5, false, "Шейкер", "Shaker")
        addGoods(6, true, "Сироп", "Syrup")
        jdbc.update("INSERT INTO drink_me_bot.goods_category (goods_id, category) VALUES (6, 'SYRUP')")

        addCocktail(100, mapOf("ru" to "Алфа", "us" to "Alpha"), listOf(1 to "ingredient", 2 to "ingredient", 5 to "gadget"))
        addCocktail(101, mapOf("ru" to "Браво"), listOf(1 to "ingredient", 3 to "ingredient"))
        addCocktail(102, mapOf("ru" to "Чарли"), listOf(1 to "ingredient", 3 to "ingredient", 4 to "ingredient"))
        addCocktail(103, mapOf("ru" to "Дельта"), listOf(4 to "ingredient", 2 to "ingredient"))
        addCocktail(104, mapOf("ru" to "Безалкогольный"), listOf(2 to "ingredient"))
        addCocktail(105, mapOf("ru" to "Эхо"), listOf(1 to "ingredient", 3 to "ingredient"))
        addCocktail(106, mapOf("ru" to "Фокстрот"), listOf(1 to "ingredient", 4 to "ingredient"))
        addCocktail(108, mapOf("ru" to "Лимонад с сиропом"), listOf(2 to "ingredient", 6 to "ingredient"))
        addCocktail(109, mapOf("ru" to "Ромовый сироп"), listOf(4 to "ingredient", 6 to "ingredient"))
        addCocktail(107, mapOf("us" to "Golf"), listOf(1 to "ingredient", 4 to "ingredient"))
    }

    should("find cocktails that can be made right now, ignoring non-essential goods and gadgets") {
        setStock(1L, 1)

        val ready = repository.findByMissing(1L, "ru", missing = 0, limit = 10, offset = 0)

        ready.map { it.name } shouldBe listOf("Алфа")
        ready.single().matchedCount shouldBe 1
        ready.single().missingGoods.shouldBeEmpty()
    }

    should("find cocktails missing one essential ingredient with the missing goods listed") {
        setStock(1L, 1)

        val almost = repository.findByMissing(1L, "ru", missing = 1, limit = 10, offset = 0)

        almost.map { it.name } shouldBe listOf("Браво", "Фокстрот", "Эхо")
        almost.first { it.name == "Браво" }.missingGoods shouldBe listOf(liqueur)
        almost.first { it.name == "Фокстрот" }.missingGoods shouldBe listOf(rum)
    }

    should("find cocktails missing two essential ingredients") {
        setStock(1L, 1)

        val almost = repository.findByMissing(1L, "ru", missing = 2, limit = 10, offset = 0)

        almost.map { it.name } shouldBe listOf("Чарли")
        almost.single().missingGoods shouldBe listOf(liqueur, rum)
    }

    should("skip cocktails with nothing in stock and cocktails without essential ingredients") {
        setStock(1L, 1)

        val all = (0..2).flatMap { repository.findByMissing(1L, "ru", it, 10, 0) }.map { it.name }

        all.contains("Дельта") shouldBe false
        all.contains("Безалкогольный") shouldBe false
    }

    should("treat syrups as essential for matching but not as alcohol") {
        setStock(3L, 6)

        val sections = (0..2).flatMap { repository.findByMissing(3L, "ru", it, 10, 0) }.map { it.name }

        // 108 только с сиропом (безалкогольный) в подбор не попадает, 109 с ромом и сиропом - не хватает рома
        sections.contains("Лимонад с сиропом") shouldBe false
        sections.contains("Ромовый сироп") shouldBe true
        repository.findByMissing(3L, "ru", missing = 1, limit = 10, offset = 0).single { it.name == "Ромовый сироп" }.missingGoods shouldBe listOf(rum)
        repository.findByMissing(3L, "ru", missing = 0, limit = 10, offset = 0).shouldBeEmpty()

        setStock(3L, 4, 6)
        repository.findByMissing(3L, "ru", missing = 0, limit = 10, offset = 0).map { it.name } shouldBe listOf("Ромовый сироп", "Дельта")
    }

    should("page results") {
        setStock(1L, 1)

        repository.findByMissing(1L, "ru", missing = 1, limit = 2, offset = 0).map { it.name } shouldBe listOf("Браво", "Фокстрот")
        repository.findByMissing(1L, "ru", missing = 1, limit = 2, offset = 2).map { it.name } shouldBe listOf("Эхо")
    }

    should("count cocktails per section") {
        setStock(1L, 1)

        repository.countByMissing(1L, "ru", maxMissing = 2) shouldBe mapOf(0 to 1, 1 to 3, 2 to 1)
    }

    should("return empty sections when nothing is in stock") {
        setStock(2L)

        repository.countByMissing(2L, "ru", maxMissing = 2) shouldBe mapOf(0 to 0, 1 to 0, 2 to 0)
        repository.findByMissing(2L, "ru", missing = 0, limit = 10, offset = 0).shouldBeEmpty()
    }

    should("make a cocktail ready when its essential ingredients are in stock") {
        setStock(3L, 1, 3)

        repository.findByMissing(3L, "ru", missing = 0, limit = 10, offset = 0).map { it.name } shouldBe listOf("Браво", "Эхо", "Алфа")
    }

    should("keep stocks of different users independent") {
        setStock(1L, 1)
        setStock(3L, 4)

        repository.findByMissing(1L, "ru", 0, 10, 0).map { it.name } shouldBe listOf("Алфа")
        repository.findByMissing(3L, "ru", 0, 10, 0).map { it.name } shouldBe listOf("Дельта")
    }

    should("work in another locale and skip cocktails missing from it") {
        setStock(1L, 1)

        repository.findByMissing(1L, "us", 0, 10, 0).map { it.name } shouldBe listOf("Alpha")
        repository.findByMissing(1L, "us", 1, 10, 0).map { it.name } shouldBe listOf("Golf")
        repository.findByMissing(1L, "us", 1, 10, 0).single().missingGoods shouldBe listOf(GoodsRef(4, "Rum"))
    }

    should("suggest what to buy by the number of unlocked cocktails") {
        setStock(1L, 1)

        val buy = repository.findBuySuggestions(1L, "ru", limit = 5)

        buy.map { it.goods } shouldBe listOf(liqueur, rum)
        buy.map { it.unlocksCount } shouldBe listOf(2, 1)
    }

    should("not suggest anything to buy when the stock is empty") {
        setStock(2L)

        repository.findBuySuggestions(2L, "ru", limit = 5).shouldBeEmpty()
    }

    should("limit what to buy") {
        setStock(1L, 1)

        repository.findBuySuggestions(1L, "ru", limit = 1).map { it.goods } shouldBe listOf(liqueur)
    }
}
