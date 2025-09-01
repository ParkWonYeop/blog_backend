package me.wypark.blogbackend.application.dashboard

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DateTimeException
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

