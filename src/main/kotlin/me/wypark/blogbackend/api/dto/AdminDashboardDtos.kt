package me.wypark.blogbackend.api.dto

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

data class DashboardPostStat(
    val id: Long,
    val title: String,
    val slug: String,
    val categoryName: String,
    val viewCount: Long,
    val rangeViewCount: Long,
    val commentCount: Long,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime?
)

data class DashboardCategoryStat(
    val id: Long,
    val name: String,
    val parentId: Long?,
    val postCount: Long,
    val viewCount: Long,
    val recentViewCount: Long,
    val lastPublishedAt: OffsetDateTime?,
    val childrenCount: Long
)

data class DashboardActionItems(
    val unansweredComments: Long,
    val uncategorizedPosts: Long,
    val stalePopularPosts: Long
)

data class DashboardPostSummary(
    val id: Long,
    val title: String,
    val slug: String,
    val categoryName: String,
    val viewCount: Long,
    val createdAt: OffsetDateTime,
    val tags: List<String>
)

data class AdminDashboardCommentSummary(
    val id: Long,
    val content: String,
    val author: String?,
    val guestNickname: String?,
    val memberNickname: String?,
    val postSlug: String?,
    val postTitle: String?,
    val createdAt: OffsetDateTime
)

data class AdminDashboardResponse(
    val overview: DashboardOverview,
    val traffic: List<DashboardTrafficPoint>,
    val topPosts: List<DashboardPostStat>,
    val risingPosts: List<DashboardPostStat>,
    val stalePopularPosts: List<DashboardPostStat>,
    val recentPosts: List<DashboardPostSummary>,
    val recentComments: List<AdminDashboardCommentSummary>,
    val categoryStats: List<DashboardCategoryStat>,
    val actionItems: DashboardActionItems
)
