package me.wypark.blogbackend.domain.stats.service

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

data class BlogStatsSummary(
    val monthlyPostCount: Long,
    val monthlyViewCount: Long,
    val totalPostCount: Long
)

@Service
class BlogStatsService(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    private val clock: Clock
) {

    fun summary(): BlogStatsSummary {
        val monthStart = LocalDate.now(clock.withZone(KOREA_ZONE_ID)).withDayOfMonth(1)
        val params = MapSqlParameterSource().addValue("monthStart", monthStart)

        val monthlyPosts = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM post WHERE created_at >= :monthStart",
            params,
            Long::class.java
        ) ?: 0L
        val monthlyViews = jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(view_count), 0) FROM post_view_daily_stats WHERE stat_date >= :monthStart",
            params,
            Long::class.java
        ) ?: 0L
        val totalPosts = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM post",
            MapSqlParameterSource(),
            Long::class.java
        ) ?: 0L

        return BlogStatsSummary(
            monthlyPostCount = monthlyPosts,
            monthlyViewCount = monthlyViews,
            totalPostCount = totalPosts
        )
    }

    companion object {
        private val KOREA_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
