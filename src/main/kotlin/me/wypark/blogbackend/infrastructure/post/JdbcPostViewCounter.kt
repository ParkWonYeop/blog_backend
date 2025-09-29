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
