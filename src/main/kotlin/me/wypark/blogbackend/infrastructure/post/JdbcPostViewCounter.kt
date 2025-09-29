package me.wypark.blogbackend.infrastructure.post

import me.wypark.blogbackend.application.post.PostViewCounter
import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.time.LocalDate
import javax.sql.DataSource

@Repository
class JdbcPostViewCounter(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    private val dataSource: DataSource
) : PostViewCounter {

    private val isPostgreSql: Boolean by lazy {
        dataSource.connection.use { connection ->
            connection.metaData.databaseProductName.contains("PostgreSQL", ignoreCase = true)
        }
    }

    override fun increment(postId: Long, date: LocalDate) {
        if (isPostgreSql) {
            incrementPostViewWithPostgresUpsert(postId, date)
            return
        }

        incrementPostViewGenerically(postId, date)
    }

    private fun incrementPostViewWithPostgresUpsert(postId: Long, statDate: LocalDate) {
        jdbcTemplate.update(
            """
            INSERT INTO post_view_daily_stats (post_id, stat_date, view_count, created_at, updated_at)
            VALUES (:postId, :statDate, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT (post_id, stat_date)
            DO UPDATE SET
                view_count = post_view_daily_stats.view_count + 1,
                updated_at = CURRENT_TIMESTAMP
            """.trimIndent(),
            params(postId, statDate)
        )
    }

    private fun incrementPostViewGenerically(postId: Long, statDate: LocalDate) {
        val updatedRows = updateExistingRow(postId, statDate)
        if (updatedRows > 0) return

