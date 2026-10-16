package ru.grabovsky.drinkmebot.service

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import ru.grabovsky.drinkmebot.model.STATS_ZONE
import ru.grabovsky.drinkmebot.repository.StatsRepository
import java.time.LocalDate

class AudienceMetricsTest : ShouldSpec({
    should("expose the audience as gauges read from the repository on every scrape") {
        val today = LocalDate.now(STATS_ZONE)
        var total = 10L
        val stats = mockk<StatsRepository> {
            every { countUsers() } answers { total }
            every { countActiveSince(today) } returns 3
            every { countActiveSince(today.minusDays(6)) } returns 7
        }
        val registry = SimpleMeterRegistry()
        AudienceMetrics(stats).bindTo(registry)

        registry.get("drinkme.users.total").gauge().value() shouldBe 10.0
        total = 12
        registry.get("drinkme.users.total").gauge().value() shouldBe 12.0
        registry.get("drinkme.users.active").tag("period", "day").gauge().value() shouldBe 3.0
        registry.get("drinkme.users.active").tag("period", "week").gauge().value() shouldBe 7.0
    }
})
