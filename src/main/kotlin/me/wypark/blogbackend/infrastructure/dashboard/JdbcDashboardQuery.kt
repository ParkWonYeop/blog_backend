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

    override fun findTrafficBetween(startDate: LocalDate, endDate: LocalDate): List<DashboardTrafficRow> {
        return jdbcTemplate.query(
            """
            SELECT stat_date, COALESCE(SUM(view_count), 0) AS views
            FROM post_view_daily_stats
            WHERE stat_date BETWEEN :startDate AND :endDate
            GROUP BY stat_date
            ORDER BY stat_date ASC
            """.trimIndent(),
            dateRangeParams(startDate, endDate)
        ) { rs, _ ->
            DashboardTrafficRow(
                date = rs.getDate("stat_date").toLocalDate(),
                views = rs.getLong("views")
            )
        }
    }

    override fun findTopPosts(startDate: LocalDate, endDate: LocalDate, limit: Int): List<DashboardPostStatRow> {
        return jdbcTemplate.query(
            """
            WITH current_views AS (
                SELECT post_id, SUM(view_count) AS view_count
                FROM post_view_daily_stats
                WHERE stat_date BETWEEN :startDate AND :endDate
                GROUP BY post_id
            )
            SELECT p.id,
                   p.title,
                   p.slug,
                   COALESCE(c.name, '미분류') AS category_name,
                   p.view_count,
                   COALESCE(cv.view_count, 0) AS range_view_count,
                   COUNT(DISTINCT cm.id) AS comment_count,
                   p.created_at,
                   p.updated_at
            FROM post p
            JOIN current_views cv ON cv.post_id = p.id
            LEFT JOIN category c ON c.id = p.category_id
            LEFT JOIN comment cm ON cm.post_id = p.id
            GROUP BY p.id, p.title, p.slug, c.name, p.view_count, cv.view_count, p.created_at, p.updated_at
            ORDER BY range_view_count DESC, p.id DESC
            LIMIT :limit
            """.trimIndent(),
            dateRangeParams(startDate, endDate).addValue("limit", limit),
            postStatRowMapper
        )
    }
