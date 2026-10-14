package ru.grabovsky.drinkmebot.service

import io.kotest.core.spec.style.ShouldSpec
import io.mockk.mockk
import io.mockk.verify
import ru.grabovsky.drinkmebot.repository.StatsRepository
import java.time.Instant
import java.time.LocalDate

class StatsServiceImplTest : ShouldSpec({
    val repository = mockk<StatsRepository>(relaxed = true)
    val service = StatsServiceImpl(repository)
    // 12:00 МСК 15 июня: в тот же день по МСК, но 21:30 UTC предыдущих суток - уже следующий день
    val now = Instant.parse("2026-06-15T09:00:00Z")

    should("record the first activity of a new user") {
        service.recordActivity(1L, null, now)
        verify { repository.touchActivity(1L, LocalDate.parse("2026-06-15")) }
    }

    should("not record activity twice on the same day in Moscow time") {
        service.recordActivity(2L, Instant.parse("2026-06-14T21:30:00Z"), now)
        verify(exactly = 0) { repository.touchActivity(2L, any()) }
    }

    should("record activity when the previous action was on another day") {
        service.recordActivity(3L, Instant.parse("2026-06-14T10:00:00Z"), now)
        verify { repository.touchActivity(3L, LocalDate.parse("2026-06-15")) }
    }

    should("count a cocktail view") {
        service.recordCocktailView(42)
        verify { repository.addCocktailView(42) }
    }
})
