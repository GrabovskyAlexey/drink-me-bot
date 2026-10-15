package ru.grabovsky.drinkmebot.service

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import ru.grabovsky.drinkmebot.model.CocktailMatch
import ru.grabovsky.drinkmebot.repository.CocktailCollectionRepository
import ru.grabovsky.drinkmebot.repository.CocktailSuggestRepository
import kotlin.random.Random

class CocktailCollectionServiceImplTest : ShouldSpec({
    val collections = mockk<CocktailCollectionRepository>(relaxed = true)
    val suggestions = mockk<CocktailSuggestRepository>()
    val service = CocktailCollectionServiceImpl(collections, suggestions)
    val cocktail = CocktailMatch(5, "Алфа", 1, 0, emptyList())

    should("return nothing when no cocktail is ready") {
        every { suggestions.countByMissing(7L, "ru", 0) } returns mapOf(0 to 0)

        service.randomReady(7L, "ru").shouldBeNull()
    }

    should("pick a cocktail from the ready section by a random offset") {
        every { suggestions.countByMissing(7L, "ru", 0) } returns mapOf(0 to 3)
        every { suggestions.findByMissing(7L, "ru", 0, 1, any()) } returns listOf(cocktail)

        service.randomReady(7L, "ru", Random(1)) shouldBe cocktail

        verify { suggestions.findByMissing(7L, "ru", 0, 1, match { it in 0..2 }) }
    }

    should("convert a page number to an offset") {
        service.nonAlcoholic("ru", page = 2, pageSize = 8)

        verify { collections.findNonAlcoholic("ru", 8, 16) }
    }
})
