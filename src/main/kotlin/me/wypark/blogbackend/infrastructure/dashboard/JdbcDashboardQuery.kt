package me.wypark.blogbackend.infrastructure.dashboard

import me.wypark.blogbackend.application.dashboard.DashboardCategoryStatRow
import me.wypark.blogbackend.application.dashboard.DashboardPostStatRow
import me.wypark.blogbackend.application.dashboard.DashboardQuery
import me.wypark.blogbackend.application.dashboard.DashboardTrafficRow
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.LocalDate
import java.time.LocalDateTime

@Repository
class JdbcDashboardQuery(
    private val jdbcTemplate: NamedParameterJdbcTemplate
) : DashboardQuery {

    override fun sumViewsBetween(startDate: LocalDate, endDate: LocalDate): Long {
        return queryLong(
            """
            SELECT COALESCE(SUM(view_count), 0)
            FROM post_view_daily_stats
            WHERE stat_date BETWEEN :startDate AND :endDate
            """.trimIndent(),
            dateRangeParams(startDate, endDate)
        )
    }

