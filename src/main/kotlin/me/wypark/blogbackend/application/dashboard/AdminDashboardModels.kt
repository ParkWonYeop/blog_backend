package me.wypark.blogbackend.application.dashboard

import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime

data class DashboardMetric(
    val value: Long,
    val previousValue: Long? = null,
    val changeRate: BigDecimal? = null
)

data class DashboardOverview(
    val todayViews: DashboardMetric,
    val weekViews: DashboardMetric,
    val monthViews: DashboardMetric,
    val totalPosts: Long,
    val totalComments: Long,
    val totalCategories: Long,
    val lastPublishedAt: OffsetDateTime?,
    val generatedAt: OffsetDateTime
)

data class DashboardTrafficPoint(
    val date: LocalDate,
    val views: Long
)

