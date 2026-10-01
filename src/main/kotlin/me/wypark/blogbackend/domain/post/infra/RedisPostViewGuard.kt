package me.wypark.blogbackend.domain.post.infra

import me.wypark.blogbackend.domain.post.service.PostViewGuard
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Duration
import java.time.LocalDate

@Repository
class RedisPostViewGuard(
    private val redisTemplate: StringRedisTemplate
) : PostViewGuard {

    override fun shouldCount(postId: Long, date: LocalDate, viewerId: String): Boolean {
        val viewerHash = MessageDigest.getInstance("SHA-256")
            .digest(viewerId.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return try {
            redisTemplate.opsForValue().setIfAbsent(
                "post-view:$postId:$date:$viewerHash",
                "1",
                Duration.ofDays(2)
            ) != false
        } catch (exception: RuntimeException) {
            log.debug("Post view de-duplication is unavailable", exception)
            true
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(RedisPostViewGuard::class.java)
    }
}
