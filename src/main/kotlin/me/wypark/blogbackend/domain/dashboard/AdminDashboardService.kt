package me.wypark.blogbackend.domain.dashboard

import me.wypark.blogbackend.api.dto.AdminDashboardCommentSummary
import me.wypark.blogbackend.api.dto.AdminDashboardResponse
import me.wypark.blogbackend.api.dto.DashboardActionItems
import me.wypark.blogbackend.api.dto.DashboardCategoryStat
import me.wypark.blogbackend.api.dto.DashboardMetric
import me.wypark.blogbackend.api.dto.DashboardOverview
import me.wypark.blogbackend.api.dto.DashboardPostStat
import me.wypark.blogbackend.api.dto.DashboardPostSummary
import me.wypark.blogbackend.api.dto.DashboardTrafficPoint
import me.wypark.blogbackend.domain.category.CategoryRepository
import me.wypark.blogbackend.domain.comment.Comment
import me.wypark.blogbackend.domain.comment.CommentRepository
import me.wypark.blogbackend.domain.post.Post
import me.wypark.blogbackend.domain.post.PostRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId

@Service
@Transactional(readOnly = true)
class AdminDashboardService(
    private val dashboardQueryRepository: AdminDashboardQueryRepository,
    private val postRepository: PostRepository,
    private val commentRepository: CommentRepository,
    private val categoryRepository: CategoryRepository,
    private val clock: Clock
) {

    fun getDashboard(rangeValue: String?, timezoneValue: String?): AdminDashboardResponse {
        val range = DashboardRange.from(rangeValue)
        val zoneId = DashboardDateUtils.resolveZoneId(timezoneValue)
        val today = LocalDate.now(clock.withZone(zoneId))
        val selectedWindow = DashboardDateUtils.currentWindow(today, range.days)
        val previousSelectedWindow = DashboardDateUtils.previousWindow(selectedWindow)
        val staleBefore = today.minusDays(STALE_POPULAR_DAYS).atStartOfDay()

        val stalePopularPosts = dashboardQueryRepository.findStalePopularPosts(
            startDate = selectedWindow.startDate,
            endDate = selectedWindow.endDate,
            staleBefore = staleBefore,
            minimumViewCount = STALE_POPULAR_MINIMUM_VIEWS,
            limit = POST_WIDGET_LIMIT
        ).map { it.toDashboardPostStat(zoneId) }

        return AdminDashboardResponse(
            overview = buildOverview(today, zoneId),
            traffic = buildTraffic(selectedWindow),
            topPosts = dashboardQueryRepository.findTopPosts(
                selectedWindow.startDate,
                selectedWindow.endDate,
                POST_WIDGET_LIMIT
            ).map { it.toDashboardPostStat(zoneId) },
            risingPosts = dashboardQueryRepository.findRisingPosts(
                currentStartDate = selectedWindow.startDate,
                currentEndDate = selectedWindow.endDate,
                previousStartDate = previousSelectedWindow.startDate,
                previousEndDate = previousSelectedWindow.endDate,
                minimumViewCount = RISING_POST_MINIMUM_VIEWS,
                limit = POST_WIDGET_LIMIT
            ).map { it.toDashboardPostStat(zoneId) },
            stalePopularPosts = stalePopularPosts,
            recentPosts = findRecentPosts(zoneId),
            recentComments = findRecentComments(zoneId),
            categoryStats = dashboardQueryRepository.findCategoryStats(
                selectedWindow.startDate,
                selectedWindow.endDate
            ).map { it.toDashboardCategoryStat(zoneId) },
            actionItems = DashboardActionItems(
                unansweredComments = dashboardQueryRepository.countUnansweredComments(),
                uncategorizedPosts = dashboardQueryRepository.countUncategorizedPosts(),
                stalePopularPosts = dashboardQueryRepository.countStalePopularPosts(
                    startDate = selectedWindow.startDate,
                    endDate = selectedWindow.endDate,
                    staleBefore = staleBefore,
                    minimumViewCount = STALE_POPULAR_MINIMUM_VIEWS
                )
            )
        )
    }

    private fun buildOverview(today: LocalDate, zoneId: ZoneId): DashboardOverview {
        val todayWindow = DashboardDateUtils.currentWindow(today, 1)
        val yesterdayWindow = DashboardDateUtils.previousWindow(todayWindow)
        val weekWindow = DashboardDateUtils.currentWindow(today, 7)
        val previousWeekWindow = DashboardDateUtils.previousWindow(weekWindow)
        val monthWindow = DashboardDateUtils.currentWindow(today, 30)
        val previousMonthWindow = DashboardDateUtils.previousWindow(monthWindow)

        return DashboardOverview(
            todayViews = metric(todayWindow, yesterdayWindow),
            weekViews = metric(weekWindow, previousWeekWindow),
            monthViews = metric(monthWindow, previousMonthWindow),
            totalPosts = postRepository.count(),
            totalComments = commentRepository.count(),
            totalCategories = categoryRepository.count(),
            lastPublishedAt = dashboardQueryRepository.findLastPublishedAt().toOffsetDateTimeOrNull(zoneId),
            generatedAt = OffsetDateTime.now(clock.withZone(zoneId))
        )
    }

    private fun metric(currentWindow: DashboardDateWindow, previousWindow: DashboardDateWindow): DashboardMetric {
        val currentValue = dashboardQueryRepository.sumViewsBetween(currentWindow.startDate, currentWindow.endDate)
        val previousValue = dashboardQueryRepository.sumViewsBetween(previousWindow.startDate, previousWindow.endDate)

        return DashboardMetric(
            value = currentValue,
            previousValue = previousValue,
            changeRate = DashboardDateUtils.changeRate(currentValue, previousValue)
        )
    }

    private fun buildTraffic(window: DashboardDateWindow): List<DashboardTrafficPoint> {
        val viewsByDate = dashboardQueryRepository.findTrafficBetween(window.startDate, window.endDate)
            .associate { it.date to it.views }

        val points = mutableListOf<DashboardTrafficPoint>()
        var date = window.startDate
        while (!date.isAfter(window.endDate)) {
            points.add(DashboardTrafficPoint(date = date, views = viewsByDate[date] ?: 0L))
            date = date.plusDays(1)
        }
        return points
    }

    private fun findRecentPosts(zoneId: ZoneId): List<DashboardPostSummary> {
        val pageable = PageRequest.of(
            0,
            RECENT_WIDGET_LIMIT,
            Sort.by(Sort.Direction.DESC, "createdAt")
        )

        return postRepository.findAll(pageable).content.map { it.toDashboardPostSummary(zoneId) }
    }

    private fun findRecentComments(zoneId: ZoneId): List<AdminDashboardCommentSummary> {
        val pageable = PageRequest.of(
            0,
            RECENT_WIDGET_LIMIT,
            Sort.by(Sort.Direction.DESC, "createdAt")
        )

        return commentRepository.findAll(pageable).content.map { it.toDashboardCommentSummary(zoneId) }
    }

    private fun DashboardPostStatRow.toDashboardPostStat(zoneId: ZoneId): DashboardPostStat {
        return DashboardPostStat(
            id = id,
            title = title,
            slug = slug,
            categoryName = categoryName,
            viewCount = viewCount,
            rangeViewCount = rangeViewCount,
            commentCount = commentCount,
            createdAt = createdAt.toOffsetDateTime(zoneId),
            updatedAt = updatedAt.toOffsetDateTimeOrNull(zoneId)
        )
    }

    private fun DashboardCategoryStatRow.toDashboardCategoryStat(zoneId: ZoneId): DashboardCategoryStat {
        return DashboardCategoryStat(
            id = id,
            name = name,
            parentId = parentId,
            postCount = postCount,
            viewCount = viewCount,
            recentViewCount = recentViewCount,
            lastPublishedAt = lastPublishedAt.toOffsetDateTimeOrNull(zoneId),
            childrenCount = childrenCount
        )
    }

    private fun Post.toDashboardPostSummary(zoneId: ZoneId): DashboardPostSummary {
        return DashboardPostSummary(
            id = id!!,
            title = title,
            slug = slug,
            categoryName = category?.name ?: "미분류",
            viewCount = viewCount,
            createdAt = createdAt.toOffsetDateTime(zoneId),
            tags = tags.map { it.tag.name }
        )
    }

    private fun Comment.toDashboardCommentSummary(zoneId: ZoneId): AdminDashboardCommentSummary {
        return AdminDashboardCommentSummary(
            id = id!!,
            content = content,
            author = getAuthorName(),
            guestNickname = guestNickname,
            memberNickname = member?.nickname,
            postSlug = post.slug,
            postTitle = post.title,
            createdAt = createdAt.toOffsetDateTime(zoneId)
        )
    }

    private fun LocalDateTime.toOffsetDateTime(zoneId: ZoneId): OffsetDateTime {
        return atZone(zoneId).toOffsetDateTime()
    }

    private fun LocalDateTime?.toOffsetDateTimeOrNull(zoneId: ZoneId): OffsetDateTime? {
        return this?.toOffsetDateTime(zoneId)
    }

    companion object {
        private const val POST_WIDGET_LIMIT = 5
        private const val RECENT_WIDGET_LIMIT = 5
        private const val RISING_POST_MINIMUM_VIEWS = 5L
        private const val STALE_POPULAR_MINIMUM_VIEWS = 10L
        private const val STALE_POPULAR_DAYS = 180L
    }
}
