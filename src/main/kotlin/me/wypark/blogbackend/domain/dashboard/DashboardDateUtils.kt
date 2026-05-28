package me.wypark.blogbackend.domain.dashboard

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

enum class DashboardRange(val queryValue: String, val days: Long) {
    SEVEN_DAYS("7d", 7),
    THIRTY_DAYS("30d", 30),
    NINETY_DAYS("90d", 90);

    companion object {
        fun from(rawValue: String?): DashboardRange {
            return entries.firstOrNull { it.queryValue == rawValue?.lowercase() } ?: THIRTY_DAYS
        }
    }
}

data class DashboardDateWindow(
    val startDate: LocalDate,
    val endDate: LocalDate
) {
    val days: Long = ChronoUnit.DAYS.between(startDate, endDate) + 1
}

object DashboardDateUtils {
    private val defaultZoneId: ZoneId = ZoneId.of("Asia/Seoul")

    fun resolveZoneId(rawValue: String?): ZoneId {
        if (rawValue.isNullOrBlank()) return defaultZoneId

        return try {
            ZoneId.of(rawValue)
        } catch (e: Exception) {
            defaultZoneId
        }
    }

    fun currentWindow(today: LocalDate, days: Long): DashboardDateWindow {
        return DashboardDateWindow(
            startDate = today.minusDays(days - 1),
            endDate = today
        )
    }

    fun previousWindow(currentWindow: DashboardDateWindow): DashboardDateWindow {
        val previousEndDate = currentWindow.startDate.minusDays(1)
        return DashboardDateWindow(
            startDate = previousEndDate.minusDays(currentWindow.days - 1),
            endDate = previousEndDate
        )
    }

    fun changeRate(currentValue: Long, previousValue: Long): BigDecimal? {
        if (previousValue == 0L) return null

        return BigDecimal.valueOf(currentValue - previousValue)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(previousValue), 2, RoundingMode.HALF_UP)
    }
}
