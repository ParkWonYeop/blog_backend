package me.wypark.blogbackend.api.controller.admin

import jakarta.persistence.EntityManager
import me.wypark.blogbackend.domain.category.Category
import me.wypark.blogbackend.domain.category.CategoryRepository
import me.wypark.blogbackend.domain.comment.Comment
import me.wypark.blogbackend.domain.comment.CommentRepository
import me.wypark.blogbackend.domain.post.Post
import me.wypark.blogbackend.domain.post.PostRepository
import me.wypark.blogbackend.application.post.PostViewCounter
import me.wypark.blogbackend.domain.user.Member
import me.wypark.blogbackend.domain.user.MemberRepository
import me.wypark.blogbackend.domain.user.Role
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.test.assertEquals

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminDashboardIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var memberRepository: MemberRepository

    @Autowired
    lateinit var categoryRepository: CategoryRepository

    @Autowired
    lateinit var postRepository: PostRepository

    @Autowired
    lateinit var commentRepository: CommentRepository

    @Autowired
    lateinit var postViewCounter: PostViewCounter

    @Autowired
    lateinit var jdbcTemplate: NamedParameterJdbcTemplate

    @Autowired
    lateinit var entityManager: EntityManager

    @Test
    fun `post detail view creates and increments daily stats`() {
        val admin = saveMember("admin@example.com", Role.ROLE_ADMIN)
        val post = postRepository.saveAndFlush(
            Post(
                title = "Daily Stats",
                content = "content",
                slug = "daily-stats",
                member = admin
            )
        )
        val postId = requireNotNull(post.id)
        val today = LocalDate.now(ZoneId.of("Asia/Seoul"))

        mockMvc.perform(get("/api/posts/{slug}", post.slug))
            .andExpect(status().isOk)

        mockMvc.perform(get("/api/posts/{slug}", post.slug))
            .andExpect(status().isOk)

        entityManager.flush()
        entityManager.clear()

        assertEquals(2L, findDailyViewCount(postId, today))
        assertEquals(2L, postRepository.findBySlug(post.slug)?.viewCount)
    }

    @Test
    fun `dashboard requires admin authority`() {
        mockMvc.perform(get("/api/admin/dashboard"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))

        mockMvc.perform(get("/api/admin/dashboard").with(user("user").roles("USER")))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("FORBIDDEN"))

        mockMvc.perform(get("/api/admin/dashboard").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.message").value("OK"))
    }

    @Test
    fun `dashboard returns traffic metrics post stats categories and action items`() {
        val admin = saveMember("admin-dashboard@example.com", Role.ROLE_ADMIN)
        val category = categoryRepository.saveAndFlush(Category(name = "Backend"))
        val post = postRepository.saveAndFlush(
            Post(
                title = "Dashboard API",
                content = "content",
                slug = "dashboard-api",
                viewCount = 5,
                member = admin,
                category = category
            )
        )
        val stalePost = postRepository.saveAndFlush(
            Post(
                title = "Old Popular API",
                content = "content",
                slug = "old-popular-api",
                viewCount = 10,
                member = admin,
                category = category
            )
        )
        val postId = requireNotNull(post.id)
        val stalePostId = requireNotNull(stalePost.id)
        val today = LocalDate.now(ZoneId.of("Asia/Seoul"))

        repeat(3) { postViewCounter.increment(postId, today) }
        repeat(2) { postViewCounter.increment(postId, today.minusDays(1)) }
        repeat(10) { postViewCounter.increment(stalePostId, today) }
        markPostAsOld(stalePostId, today.minusDays(181).atStartOfDay())

        val unanswered = commentRepository.saveAndFlush(
            Comment(
                content = "Can you explain this?",
                post = post,
                guestNickname = "guest",
                guestPassword = "pw"
            )
        )
        val answered = commentRepository.saveAndFlush(
            Comment(
                content = "Already answered",
                post = post,
                guestNickname = "guest2",
                guestPassword = "pw"
            )
        )
        commentRepository.saveAndFlush(
            Comment(
                content = "Admin answer",
                post = post,
                parent = answered,
                member = admin
            )
        )

        entityManager.flush()
        entityManager.clear()

        mockMvc.perform(
            get("/api/admin/dashboard")
                .param("range", "7d")
                .param("timezone", "Asia/Seoul")
                .with(user("admin").roles("ADMIN"))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.traffic.length()").value(7))
            .andExpect(jsonPath("$.data.overview.todayViews.value").value(13))
            .andExpect(jsonPath("$.data.overview.weekViews.value").value(15))
            .andExpect(jsonPath("$.data.topPosts[0].slug").value(stalePost.slug))
            .andExpect(jsonPath("$.data.risingPosts[0].rangeViewCount").value(10))
            .andExpect(jsonPath("$.data.stalePopularPosts[0].slug").value(stalePost.slug))
            .andExpect(jsonPath("$.data.categoryStats[0].name").value(category.name))
            .andExpect(jsonPath("$.data.categoryStats[0].postCount").value(2))
            .andExpect(jsonPath("$.data.categoryStats[0].recentViewCount").value(15))
            .andExpect(jsonPath("$.data.actionItems.unansweredComments").value(1))
            .andExpect(jsonPath("$.data.actionItems.stalePopularPosts").value(1))

        val unansweredId = requireNotNull(unanswered.id)
        assertEquals(unansweredId, commentRepository.findById(unansweredId).orElseThrow().id)
    }

    private fun saveMember(email: String, role: Role): Member {
        return memberRepository.saveAndFlush(
            Member(
                email = email,
                password = "password",
                nickname = email.substringBefore("@"),
                role = role,
                isVerified = true
            )
        )
    }

    private fun findDailyViewCount(postId: Long, date: LocalDate): Long {
        return jdbcTemplate.queryForObject(
            """
            SELECT view_count
            FROM post_view_daily_stats
            WHERE post_id = :postId
              AND stat_date = :statDate
            """.trimIndent(),
            MapSqlParameterSource()
                .addValue("postId", postId)
                .addValue("statDate", date),
            Number::class.java
        ).let(::requireNotNull).toLong()
    }

    private fun markPostAsOld(postId: Long, dateTime: LocalDateTime) {
        jdbcTemplate.update(
            """
            UPDATE post
            SET created_at = :dateTime,
                updated_at = :dateTime
            WHERE id = :postId
            """.trimIndent(),
            MapSqlParameterSource()
                .addValue("postId", postId)
                .addValue("dateTime", dateTime)
        )
    }
}
