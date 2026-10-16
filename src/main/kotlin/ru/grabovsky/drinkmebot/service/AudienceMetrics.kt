package ru.grabovsky.drinkmebot.service

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.MeterBinder
import org.springframework.stereotype.Component
import ru.grabovsky.drinkmebot.model.STATS_ZONE
import ru.grabovsky.drinkmebot.repository.StatsRepository
import java.time.LocalDate

/** Аудитория бота как метрики: всего пользователей и активных за сегодня (по МСК) и за последние 7 дней. Читаются из БД при каждом сборе метрик. */
@Component
class AudienceMetrics(
    private val stats: StatsRepository,
) : MeterBinder {

    override fun bindTo(registry: MeterRegistry) {
        Gauge.builder("drinkme.users.total") { stats.countUsers().toDouble() }
            .description("Всего пользователей")
            .register(registry)
        Gauge.builder("drinkme.users.active") { stats.countActiveSince(today()).toDouble() }
            .tag("period", "day").description("Активных пользователей за период").register(registry)
        Gauge.builder("drinkme.users.active") { stats.countActiveSince(today().minusDays(6)).toDouble() }
            .tag("period", "week").description("Активных пользователей за период").register(registry)
    }

    private fun today(): LocalDate = LocalDate.now(STATS_ZONE)
}
