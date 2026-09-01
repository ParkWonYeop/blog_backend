package me.wypark.blogbackend.domain.stats.service

import me.wypark.blogbackend.domain.post.entity.Post
import me.wypark.blogbackend.domain.post.repository.PostRepository
import me.wypark.blogbackend.domain.post.service.PostViewCounter
import me.wypark.blogbackend.domain.user.entity.Member
import me.wypark.blogbackend.domain.user.entity.Role
import me.wypark.blogbackend.domain.user.repository.MemberRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BlogStatsServiceIntegrationTest {

    @Autowired
    lateinit var blogStatsService: BlogStatsService

    @Autowired
    lateinit var memberRepository: MemberRepository

    @Autowired
    lateinit var postRepository: PostRepository

    @Autowired
    lateinit var postViewCounter: PostViewCounter

    @Autowired
    lateinit var clock: Clock

    @Test
    fun `summary counts posts and views for the current month`() {
        val member = memberRepository.save(
            Member(
                email = "stats@example.com",
                password = "encoded",
                nickname = "stats",
                role = Role.ROLE_ADMIN,
                isVerified = true
            )
        )
        val post = postRepository.save(
            Post(title = "stats post", content = "body", slug = "stats-post", member = member)
        )
        val today = LocalDate.now(clock.withZone(ZoneId.of("Asia/Seoul")))
        postViewCounter.increment(requireNotNull(post.id), today)
        postViewCounter.increment(requireNotNull(post.id), today)

        val summary = blogStatsService.summary()

        assertTrue(summary.monthlyPostCount >= 1)
        assertTrue(summary.totalPostCount >= 1)
        assertEquals(2, summary.monthlyViewCount)
    }
}
