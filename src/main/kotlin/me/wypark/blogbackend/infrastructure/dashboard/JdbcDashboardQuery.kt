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

    override fun findRisingPosts(
        currentStartDate: LocalDate,
        currentEndDate: LocalDate,
        previousStartDate: LocalDate,
        previousEndDate: LocalDate,
        minimumViewCount: Long,
        limit: Int
    ): List<DashboardPostStatRow> {
        return jdbcTemplate.query(
            """
            WITH current_views AS (
                SELECT post_id, SUM(view_count) AS view_count
                FROM post_view_daily_stats
                WHERE stat_date BETWEEN :currentStartDate AND :currentEndDate
                GROUP BY post_id
            ),
            previous_views AS (
                SELECT post_id, SUM(view_count) AS view_count
                FROM post_view_daily_stats
                WHERE stat_date BETWEEN :previousStartDate AND :previousEndDate
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
            LEFT JOIN current_views cv ON cv.post_id = p.id
            LEFT JOIN previous_views pv ON pv.post_id = p.id
            LEFT JOIN category c ON c.id = p.category_id
            LEFT JOIN comment cm ON cm.post_id = p.id
            WHERE COALESCE(cv.view_count, 0) >= :minimumViewCount
            GROUP BY p.id, p.title, p.slug, c.name, p.view_count, cv.view_count, pv.view_count, p.created_at, p.updated_at
            ORDER BY (COALESCE(cv.view_count, 0) - COALESCE(pv.view_count, 0)) DESC,
                     COALESCE(cv.view_count, 0) DESC,
                     p.id DESC
            LIMIT :limit
            """.trimIndent(),
            MapSqlParameterSource()
                .addValue("currentStartDate", currentStartDate)
                .addValue("currentEndDate", currentEndDate)
                .addValue("previousStartDate", previousStartDate)
                .addValue("previousEndDate", previousEndDate)
                .addValue("minimumViewCount", minimumViewCount)
                .addValue("limit", limit),
            postStatRowMapper
        )
    }

    override fun findStalePopularPosts(
        startDate: LocalDate,
        endDate: LocalDate,
        staleBefore: LocalDateTime,
        minimumViewCount: Long,
        limit: Int
    ): List<DashboardPostStatRow> {
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
            WHERE COALESCE(cv.view_count, 0) >= :minimumViewCount
              AND p.updated_at <= :staleBefore
            GROUP BY p.id, p.title, p.slug, c.name, p.view_count, cv.view_count, p.created_at, p.updated_at
            ORDER BY COALESCE(cv.view_count, 0) DESC, p.updated_at ASC, p.id DESC
            LIMIT :limit
            """.trimIndent(),
            dateRangeParams(startDate, endDate)
                .addValue("staleBefore", staleBefore)
                .addValue("minimumViewCount", minimumViewCount)
                .addValue("limit", limit),
            postStatRowMapper
        )
    }

    override fun countStalePopularPosts(
        startDate: LocalDate,
        endDate: LocalDate,
        staleBefore: LocalDateTime,
        minimumViewCount: Long
    ): Long {
        return queryLong(
            """
            WITH current_views AS (
                SELECT post_id, SUM(view_count) AS view_count
                FROM post_view_daily_stats
                WHERE stat_date BETWEEN :startDate AND :endDate
                GROUP BY post_id
            )
            SELECT COUNT(*)
            FROM post p
            JOIN current_views cv ON cv.post_id = p.id
            WHERE COALESCE(cv.view_count, 0) >= :minimumViewCount
              AND p.updated_at <= :staleBefore
            """.trimIndent(),
            dateRangeParams(startDate, endDate)
                .addValue("staleBefore", staleBefore)
                .addValue("minimumViewCount", minimumViewCount)
        )
    }

    override fun findCategoryStats(startDate: LocalDate, endDate: LocalDate): List<DashboardCategoryStatRow> {
        return jdbcTemplate.query(
            """
            SELECT c.id,
                   c.name,
                   c.parent_id,
                   COALESCE(pc.post_count, 0) AS post_count,
                   COALESCE(pc.view_count, 0) AS view_count,
                   COALESCE(rv.recent_view_count, 0) AS recent_view_count,
                   pc.last_published_at,
                   COALESCE(cc.children_count, 0) AS children_count
            FROM category c
            LEFT JOIN (
                SELECT category_id,
                       COUNT(*) AS post_count,
                       COALESCE(SUM(view_count), 0) AS view_count,
                       MAX(created_at) AS last_published_at
                FROM post
                WHERE category_id IS NOT NULL
                GROUP BY category_id
            ) pc ON pc.category_id = c.id
            LEFT JOIN (
                SELECT p.category_id,
                       COALESCE(SUM(s.view_count), 0) AS recent_view_count
                FROM post p
                JOIN post_view_daily_stats s ON s.post_id = p.id
                WHERE p.category_id IS NOT NULL
                  AND s.stat_date BETWEEN :startDate AND :endDate
                GROUP BY p.category_id
            ) rv ON rv.category_id = c.id
            LEFT JOIN (
                SELECT parent_id, COUNT(*) AS children_count
                FROM category
                WHERE parent_id IS NOT NULL
                GROUP BY parent_id
            ) cc ON cc.parent_id = c.id
            ORDER BY c.name ASC, c.id ASC
            """.trimIndent(),
            dateRangeParams(startDate, endDate)
        ) { rs, _ ->
            DashboardCategoryStatRow(
                id = rs.getLong("id"),
                name = rs.getString("name"),
                parentId = rs.getNullableLong("parent_id"),
                postCount = rs.getLong("post_count"),
                viewCount = rs.getLong("view_count"),
                recentViewCount = rs.getLong("recent_view_count"),
                lastPublishedAt = rs.getNullableLocalDateTime("last_published_at"),
                childrenCount = rs.getLong("children_count")
            )
        }
    }

    override fun countUncategorizedPosts(): Long {
        return queryLong(
            """
            SELECT COUNT(*)
            FROM post p
            LEFT JOIN category c ON c.id = p.category_id
            WHERE p.category_id IS NULL
               OR LOWER(c.name) IN ('uncategorized', '미분류')
            """.trimIndent(),
            MapSqlParameterSource()
        )
    }

    override fun countUnansweredComments(): Long {
        return queryLong(
            """
            SELECT COUNT(*)
            FROM comment root
            JOIN post p ON p.id = root.post_id
            LEFT JOIN member root_member ON root_member.id = root.member_id
            WHERE root.parent_id IS NULL
              AND (
                  root.member_id IS NULL
                  OR (
                      root.member_id <> p.member_id
                      AND root_member.role <> 'ROLE_ADMIN'
                  )
              )
              AND NOT EXISTS (
                  SELECT 1
                  FROM comment child
                  LEFT JOIN member child_member ON child_member.id = child.member_id
                  WHERE child.parent_id = root.id
                    AND (
                        child.member_id = p.member_id
                        OR child_member.role = 'ROLE_ADMIN'
                    )
              )
            """.trimIndent(),
            MapSqlParameterSource()
        )
    }

    override fun findLastPublishedAt(): LocalDateTime? {
        return jdbcTemplate.query(
            "SELECT MAX(created_at) AS last_published_at FROM post",
            MapSqlParameterSource()
        ) { rs, _ -> rs.getNullableLocalDateTime("last_published_at") }
            .firstOrNull()
    }
