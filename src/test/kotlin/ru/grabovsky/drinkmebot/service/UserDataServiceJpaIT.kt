package ru.grabovsky.drinkmebot.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIf
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.jdbc.datasource.init.ScriptUtils
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.DockerClientFactory
import org.testcontainers.postgresql.PostgreSQLContainer
import ru.grabovsky.drinkmebot.repository.UserDataRepository
import ru.grabovsky.drinkmebot.service.interfaces.DataLocaleService
import ru.grabovsky.drinkmebot.service.interfaces.UserDataService
import java.util.Locale

/** Проверяет выгрузку и полное удаление данных пользователя на настоящем PostgreSQL (откат после каждого теста). */
@DataJpaTest(
    properties = [
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "telegram.name=test_bot",
        "telegram.token=123456:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
    ],
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(UserDataServiceImpl::class, UserDataRepository::class, UserDataServiceJpaIT.Config::class)
@EnabledIf("ru.grabovsky.drinkmebot.service.UserDataServiceJpaIT#dockerAvailable", disabledReason = "Docker is not available")
class UserDataServiceJpaIT {

    @TestConfiguration
    class Config {
        @Bean
        fun objectMapper(): ObjectMapper = jacksonObjectMapper()

        @Bean
        fun dataLocaleService(): DataLocaleService = object : DataLocaleService {
            override fun resolve(userId: Long, uiLocale: Locale) = "ru"
            override fun chosen(userId: Long): String? = null
            override fun choose(userId: Long, lang: String?) = Unit
        }
    }

    @Autowired
    lateinit var service: UserDataService

    @Autowired
    lateinit var jdbc: JdbcTemplate

    @BeforeEach
    fun prepare() {
        listOf(7L, 8L).forEach {
            jdbc.update("INSERT INTO drink_me_bot.users (id, username) VALUES (?, 'u$it')", it)
            jdbc.update("INSERT INTO drink_me_bot.user_profile (user_id) VALUES (?)", it)
            jdbc.update("INSERT INTO drink_me_bot.user_ingredient (user_id, goods_id) VALUES (?, 1)", it)
            jdbc.update("INSERT INTO drink_me_bot.user_activity_day (user_id, day) VALUES (?, CURRENT_DATE)", it)
            jdbc.update("INSERT INTO drink_me_bot.user_cocktail (user_id, cocktail_id, favorite, cooked) VALUES (?, 10, TRUE, TRUE)", it)
            jdbc.update("INSERT INTO drink_me_bot.flow_state (user_id, flow_key, step_key) VALUES (?, 'START', 'main')", it)
        }
    }

    private fun count(table: String, userId: Long) =
        jdbc.queryForObject("SELECT count(*) FROM drink_me_bot.$table WHERE user_id = ?", Long::class.java, userId)

    @Test
    fun deletesAllDataOfTheUserOnly() {
        service.deleteAll(7L)

        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM drink_me_bot.users WHERE id = 7", Long::class.java))
        listOf("user_profile", "user_ingredient", "user_activity_day", "user_cocktail", "flow_state").forEach {
            assertEquals(0L, count(it, 7L), it)
            assertEquals(1L, count(it, 8L), it)
        }
    }

    @Test
    fun deletingUnknownUserDoesNothing() {
        service.deleteAll(99L)

        assertEquals(1L, count("user_profile", 7L))
    }

    @Test
    fun exportsProfileIngredientsAndActivity() {
        val json = String(service.export(7L, Locale.forLanguageTag("ru")), Charsets.UTF_8)

        assertTrue(json.contains("\"id\" : 7"))
        assertTrue(json.contains("\"username\" : \"u7\""))
        assertTrue(json.contains("Водка"))
        assertTrue(json.contains("activityDays"))
        assertTrue(json.contains("Алфа"))
    }

    companion object {
        private val postgres: PostgreSQLContainer? =
            if (dockerAvailable()) PostgreSQLContainer("postgres:17").also { it.start() } else null

        @JvmStatic
        fun dockerAvailable(): Boolean = DockerClientFactory.instance().isDockerAvailable

        @JvmStatic
        @DynamicPropertySource
        fun datasource(registry: DynamicPropertyRegistry) {
            val container = postgres ?: return
            registry.add("spring.datasource.url") { container.jdbcUrl }
            registry.add("spring.datasource.username") { container.username }
            registry.add("spring.datasource.password") { container.password }
            registry.add("spring.datasource.driver-class-name") { "org.postgresql.Driver" }
            val dataSource = DriverManagerDataSource(container.jdbcUrl, container.username, container.password)
            listOf(
                "liquibase/mvp/1.create_tables.sql",
                "liquibase/catalog/1.create_catalog_tables.sql",
                "liquibase/catalog/4.create_user_ingredient.sql",
                "liquibase/stats/1.create_stats_tables.sql",
                "liquibase/collections/1.create_user_cocktail.sql",
            ).forEach { script -> dataSource.connection.use { ScriptUtils.executeSqlScript(it, ClassPathResource(script)) } }
            val jdbc = JdbcTemplate(dataSource)
            jdbc.update("INSERT INTO drink_me_bot.lang (code) VALUES ('ru')")
            jdbc.update("INSERT INTO drink_me_bot.cocktail (id) VALUES (10)")
            jdbc.update("INSERT INTO drink_me_bot.cocktail_i18n (cocktail_id, lang, name) VALUES (10, 'ru', 'Алфа')")
            jdbc.update("INSERT INTO drink_me_bot.goods (id, is_essential) VALUES (1, true)")
            jdbc.update("INSERT INTO drink_me_bot.goods_i18n (goods_id, lang, name, description) VALUES (1, 'ru', 'Водка', '')")
        }
    }
}
