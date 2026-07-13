package me.wypark.blogbackend.application.dashboard

import java.time.LocalDate
import java.time.LocalDateTime

data class DashboardPostStatRow(
    val id: Long,
    val title: String,
    val slug: String,
    val categoryName: String,
    val viewCount: Long,
    val rangeViewCount: Long,
    val commentCount: Long,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime?
)

data class DashboardCategoryStatRow(
    val id: Long,
    val name: String,
    val parentId: Long?,
    val postCount: Long,
    val viewCount: Long,
    val recentViewCount: Long,
    val lastPublishedAt: LocalDateTime?,
    val childrenCount: Long
)

data class DashboardTrafficRow(
    val date: LocalDate,
    val views: Long
)
