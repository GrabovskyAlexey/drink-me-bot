package ru.grabovsky.drinkmebot.service

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import ru.grabovsky.drinkmebot.entity.UserIngredient
import ru.grabovsky.drinkmebot.entity.UserIngredientId
import ru.grabovsky.drinkmebot.repository.IngredientCatalogRepository
import ru.grabovsky.drinkmebot.repository.UserIngredientRepository

class IngredientServiceImplTest : ShouldSpec({
    val catalog = mockk<IngredientCatalogRepository>(relaxed = true)
    val userIngredients = mockk<UserIngredientRepository>(relaxed = true)
    val service = IngredientServiceImpl(catalog, userIngredients)
    val id = UserIngredientId(7L, 1)

    beforeTest { clearMocks(catalog, userIngredients) }

    should("mark an ingredient that was not selected") {
        every { catalog.isSelectable(1) } returns true
        every { userIngredients.existsById(id) } returns false
        every { userIngredients.save(any<UserIngredient>()) } answers { firstArg() }

        service.toggle(7L, 1) shouldBe true

        verify { userIngredients.save(match<UserIngredient> { it.id == id }) }
    }

    should("unmark an ingredient that was selected") {
        every { catalog.isSelectable(1) } returns true
        every { userIngredients.existsById(id) } returns true

        service.toggle(7L, 1) shouldBe false

        verify { userIngredients.deleteById(id) }
    }

    should("not toggle goods that cannot be selected") {
        every { catalog.isSelectable(999) } returns false

        service.toggle(7L, 999) shouldBe null

        verify(exactly = 0) { userIngredients.save(any<UserIngredient>()) }
        verify(exactly = 0) { userIngredients.deleteById(any()) }
    }

    should("search with a trimmed and limited query") {
        every { catalog.search(7L, "ru", "ром", 30) } returns emptyList()

        service.search(7L, "ru", "  ром ") shouldBe emptyList()

        verify { catalog.search(7L, "ru", "ром", 30) }
    }

    should("not search for a blank query") {
        service.search(7L, "ru", "   ") shouldBe emptyList()

        verify(exactly = 0) { catalog.search(any(), any(), any(), any()) }
    }

    should("reset all marks and report their number") {
        every { userIngredients.deleteAllByUserId(7L) } returns 3

        service.reset(7L) shouldBe 3
    }
})
