package me.wypark.blogbackend.domain.dashboard

import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DashboardDateUtilsTest {

    @Test
    fun `range falls back to 30d when invalid`() {
        assertEquals(DashboardRange.SEVEN_DAYS, DashboardRange.from("7d"))
        assertEquals(DashboardRange.THIRTY_DAYS, DashboardRange.from("wrong"))
        assertEquals(DashboardRange.THIRTY_DAYS, DashboardRange.from(null))
    }

    @Test
    fun `date windows include today`() {
        val today = LocalDate.of(2026, 5, 28)
        val currentWindow = DashboardDateUtils.currentWindow(today, 7)
        val previousWindow = DashboardDateUtils.previousWindow(currentWindow)

        assertEquals(LocalDate.of(2026, 5, 22), currentWindow.startDate)
        assertEquals(today, currentWindow.endDate)
        assertEquals(LocalDate.of(2026, 5, 15), previousWindow.startDate)
        assertEquals(LocalDate.of(2026, 5, 21), previousWindow.endDate)
    }

    @Test
    fun `change rate rounds to two decimals and hides zero previous value`() {
        assertEquals(BigDecimal("12.27"), DashboardDateUtils.changeRate(311, 277))
        assertEquals(BigDecimal("-25.00"), DashboardDateUtils.changeRate(75, 100))
        assertNull(DashboardDateUtils.changeRate(10, 0))
    }
}
