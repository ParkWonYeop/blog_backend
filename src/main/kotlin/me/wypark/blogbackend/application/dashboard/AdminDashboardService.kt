package me.wypark.blogbackend.application.dashboard

import me.wypark.blogbackend.domain.category.CategoryRepository
import me.wypark.blogbackend.domain.comment.CommentRepository
import me.wypark.blogbackend.domain.post.PostRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

@Service
@Transactional(readOnly = true)
class AdminDashboardService(
    private val dashboardQuery: DashboardQuery,
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

        val stalePopularPosts = dashboardQuery.findStalePopularPosts(
            startDate = selectedWindow.startDate,
            endDate = selectedWindow.endDate,
            staleBefore = staleBefore,
            minimumViewCount = STALE_POPULAR_MINIMUM_VIEWS,
            limit = POST_WIDGET_LIMIT
        ).map { it.toDashboardPostStat(zoneId) }

        return AdminDashboardResponse(
            overview = buildOverview(today, zoneId),
            traffic = buildTraffic(selectedWindow),
            topPosts = dashboardQuery.findTopPosts(
                selectedWindow.startDate,
                selectedWindow.endDate,
                POST_WIDGET_LIMIT
            ).map { it.toDashboardPostStat(zoneId) },
            risingPosts = dashboardQuery.findRisingPosts(
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
            categoryStats = dashboardQuery.findCategoryStats(
                selectedWindow.startDate,
                selectedWindow.endDate
            ).map { it.toDashboardCategoryStat(zoneId) },
            actionItems = DashboardActionItems(
                unansweredComments = dashboardQuery.countUnansweredComments(),
                uncategorizedPosts = dashboardQuery.countUncategorizedPosts(),
                stalePopularPosts = dashboardQuery.countStalePopularPosts(
                    startDate = selectedWindow.startDate,
                    endDate = selectedWindow.endDate,
                    staleBefore = staleBefore,
                    minimumViewCount = STALE_POPULAR_MINIMUM_VIEWS
                )
            )
        )
    }
