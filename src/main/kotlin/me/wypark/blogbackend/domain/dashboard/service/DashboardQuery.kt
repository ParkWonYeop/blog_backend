package me.wypark.blogbackend.domain.dashboard.service
import me.wypark.blogbackend.domain.dashboard.dto.DashboardCategoryStatRow
import me.wypark.blogbackend.domain.dashboard.dto.DashboardPostStatRow
import me.wypark.blogbackend.domain.dashboard.dto.DashboardTrafficRow

import java.time.LocalDate
import java.time.LocalDateTime

interface DashboardQuery {
    fun sumViewsBetween(startDate: LocalDate, endDate: LocalDate): Long
    fun findTrafficBetween(startDate: LocalDate, endDate: LocalDate): List<DashboardTrafficRow>
    fun findTopPosts(startDate: LocalDate, endDate: LocalDate, limit: Int): List<DashboardPostStatRow>
    fun findRisingPosts(
        currentStartDate: LocalDate,
        currentEndDate: LocalDate,
        previousStartDate: LocalDate,
        previousEndDate: LocalDate,
        minimumViewCount: Long,
        limit: Int
    ): List<DashboardPostStatRow>

    fun findStalePopularPosts(
        startDate: LocalDate,
        endDate: LocalDate,
        staleBefore: LocalDateTime,
        minimumViewCount: Long,
        limit: Int
    ): List<DashboardPostStatRow>

    fun countStalePopularPosts(
        startDate: LocalDate,
        endDate: LocalDate,
        staleBefore: LocalDateTime,
        minimumViewCount: Long
    ): Long

    fun findCategoryStats(startDate: LocalDate, endDate: LocalDate): List<DashboardCategoryStatRow>
    fun countUncategorizedPosts(): Long
    fun countUnansweredComments(): Long
    fun findLastPublishedAt(): LocalDateTime?
}
