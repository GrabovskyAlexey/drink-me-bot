package ru.grabovsky.drinkmebot.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.init.ScriptUtils
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.DockerClientFactory
import org.testcontainers.postgresql.PostgreSQLContainer
import ru.grabovsky.drinkmebot.repository.IngredientCatalogRepository
import ru.grabovsky.drinkmebot.service.interfaces.IngredientService
import org.junit.jupiter.api.condition.EnabledIf

/** Проверяет JPA-отображение `user_ingredient` и транзакции сервиса на настоящем PostgreSQL (откат после каждого теста). */
@DataJpaTest(
    properties = [
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "telegram.name=test_bot",
        "telegram.token=123456:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
    ],
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(IngredientServiceImpl::class, IngredientCatalogRepository::class)
@EnabledIf("ru.grabovsky.drinkmebot.service.IngredientServiceJpaIT#dockerAvailable", disabledReason = "Docker is not available")
class IngredientServiceJpaIT {

    @Autowired
    lateinit var service: IngredientService

    @Autowired
    lateinit var jdbc: JdbcTemplate

    @BeforeEach
    fun prepare() {
        jdbc.update("INSERT INTO drink_me_bot.users (id) VALUES (7) ON CONFLICT DO NOTHING")
    }

    @Test
    fun togglesIngredientOnAndOff() {
        assertEquals(0, service.selectedCount(7L))

        assertTrue(service.toggle(7L, 1)!!)
        assertEquals(1, service.selectedCount(7L))

        assertFalse(service.toggle(7L, 1)!!)
        assertEquals(0, service.selectedCount(7L))
    }

    @Test
    fun rejectsGoodsOutsideOfCategories() {
        assertNull(service.toggle(7L, 4))
        assertEquals(0, service.selectedCount(7L))
    }

    @Test
    fun resetsAllMarksOfTheUserOnly() {
        jdbc.update("INSERT INTO drink_me_bot.users (id) VALUES (8) ON CONFLICT DO NOTHING")
        service.toggle(7L, 1)
        service.toggle(7L, 2)
        service.toggle(8L, 1)

        assertEquals(2, service.reset(7L))

        assertEquals(0, service.selectedCount(7L))
        assertEquals(1, service.selectedCount(8L))
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
            val dataSource = org.springframework.jdbc.datasource.DriverManagerDataSource(container.jdbcUrl, container.username, container.password)
            listOf(
                "liquibase/mvp/1.create_tables.sql",
                "liquibase/catalog/1.create_catalog_tables.sql",
                "liquibase/catalog/4.create_user_ingredient.sql",
                "liquibase/catalog/5.create_goods_category.sql",
            ).forEach { script -> dataSource.connection.use { ScriptUtils.executeSqlScript(it, ClassPathResource(script)) } }
            val jdbc = JdbcTemplate(dataSource)
            jdbc.update("INSERT INTO drink_me_bot.goods (id, is_essential) VALUES (1, true), (2, true), (4, false)")
            jdbc.update("INSERT INTO drink_me_bot.goods_category (goods_id, category) VALUES (1, 'VODKA'), (2, 'RUM')")
        }
    }
}
