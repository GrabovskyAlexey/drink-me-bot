package ru.grabovsky.drinkmebot.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import ru.grabovsky.drinkmebot.repository.CocktailSuggestRepository

class CocktailSuggestServiceImplTest : ShouldSpec({
    val repository = mockk<CocktailSuggestRepository>(relaxed = true)
    val service = CocktailSuggestServiceImpl(repository, BotMetrics(SimpleMeterRegistry()))

    should("convert page number to offset") {
        every { repository.findByMissing(7L, "ru", 1, 8, 16) } returns emptyList()

        service.findCocktails(7L, "ru", missing = 1, page = 2, pageSize = 8) shouldBe emptyList()

        verify { repository.findByMissing(7L, "ru", 1, 8, 16) }
    }

    should("reject unsupported number of missing ingredients") {
        shouldThrow<IllegalArgumentException> { service.findCocktails(7L, "ru", missing = 3) }
        shouldThrow<IllegalArgumentException> { service.findCocktails(7L, "ru", missing = -1) }
    }

    should("reject negative page") {
        shouldThrow<IllegalArgumentException> { service.findCocktails(7L, "ru", missing = 0, page = -1) }
    }

    should("request counts for all sections up to the maximum") {
        every { repository.countByMissing(7L, "ru", 2) } returns mapOf(0 to 1, 1 to 2, 2 to 3)

        service.counts(7L, "ru") shouldBe mapOf(0 to 1, 1 to 2, 2 to 3)
    }
})
