package me.wypark.blogbackend.global.security

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import me.wypark.blogbackend.global.common.ApiResponse
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Clock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/** Distributed fixed-window limits for public and compute-heavy endpoints. */
@Component
class RequestRateLimitFilter(
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
    private val clock: Clock
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val rule = resolveRule(request)
        if (rule == null || allow(request, rule)) {
            filterChain.doFilter(request, response)
            return
        }

        response.status = HttpStatus.TOO_MANY_REQUESTS.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = StandardCharsets.UTF_8.name()
        response.setHeader("Retry-After", rule.windowSeconds.toString())
        response.writer.write(
            objectMapper.writeValueAsString(
                ApiResponse.error("요청이 너무 많습니다. 잠시 후 다시 시도해주세요.", "RATE_LIMITED")
            )
        )
    }

    private fun allow(request: HttpServletRequest, rule: RateRule): Boolean {
        val window = clock.millis() / (rule.windowSeconds * 1_000L)
        val clientHash = sha256(request.remoteAddr ?: "unknown")
        val key = "rate:${rule.id}:$clientHash:$window"

        return try {
            val count = redisTemplate.execute(
                RATE_LIMIT_SCRIPT,
                listOf(key),
                rule.windowSeconds.toString()
            )
            count <= rule.limit
        } catch (exception: RuntimeException) {
            logRedisFailure(exception)
            allowLocally(key, rule)
        }
    }

    private fun allowLocally(key: String, rule: RateRule): Boolean {
        val now = clock.millis()
        cleanupLocalWindows(now)
        val boundedKey = if (!localWindows.containsKey(key) && localWindows.size >= MAX_LOCAL_WINDOWS) {
            "overflow:${rule.id}:${now / (rule.windowSeconds * 1_000L)}"
        } else {
            key
        }
        val window = localWindows.compute(boundedKey) { _, existing ->
            if (existing == null || existing.expiresAt <= now) {
                LocalWindow(AtomicLong(0), now + rule.windowSeconds * 1_000L)
            } else {
                existing
            }
        } ?: return false
        return window.count.incrementAndGet() <= rule.limit
    }

    private fun cleanupLocalWindows(now: Long) {
        val previous = lastLocalCleanupAt.get()
        if (now - previous < LOCAL_CLEANUP_INTERVAL_MS || !lastLocalCleanupAt.compareAndSet(previous, now)) return
        localWindows.entries.removeIf { it.value.expiresAt <= now }
    }

    private fun resolveRule(request: HttpServletRequest): RateRule? {
        val method = request.method
        val path = request.requestURI
        return when {
            method == "POST" && path == "/api/auth/login" -> RateRule("login", 10, 60)
            method == "POST" && path == "/api/auth/signup" -> RateRule("signup", 5, 3_600)
            method == "POST" && path == "/api/auth/verify" -> RateRule("verify", 10, 60)
            method == "POST" && path == "/api/auth/reissue" -> RateRule("reissue", 30, 60)
            path == "/ws/chess" -> RateRule("chess-socket", 20, 60)
            path.startsWith("/api/comments") && method in MUTATING_METHODS ->
                RateRule("comments", 30, 60)
            path == "/api/comments" && method == "GET" -> RateRule("comment-list", 120, 60)
            path.startsWith("/api/chess/games") && method in MUTATING_METHODS ->
                RateRule("chess-engine", 30, 60)
            method == "GET" && path == "/api/posts" -> RateRule("post-search", 120, 60)
            method == "GET" && path.matches(POST_DETAIL_PATH) -> RateRule("post-detail", 180, 60)
            else -> null
        }
    }

    private fun logRedisFailure(exception: RuntimeException) {
        val now = clock.millis()
        val previous = lastFailureLogAt.get()
        if (now - previous >= FAILURE_LOG_INTERVAL_MS && lastFailureLogAt.compareAndSet(previous, now)) {
            log.warn("Rate limiter is unavailable; allowing requests until Redis recovers", exception)
        }
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    private data class RateRule(val id: String, val limit: Long, val windowSeconds: Long)
    private data class LocalWindow(val count: AtomicLong, val expiresAt: Long)

    companion object {
        private val log = LoggerFactory.getLogger(RequestRateLimitFilter::class.java)
        private val MUTATING_METHODS = setOf("POST", "PUT", "PATCH", "DELETE")
        private val POST_DETAIL_PATH = Regex("^/api/posts/[^/]+$")
        private const val FAILURE_LOG_INTERVAL_MS = 60_000L
        private const val LOCAL_CLEANUP_INTERVAL_MS = 60_000L
        private const val MAX_LOCAL_WINDOWS = 10_000
        private val lastFailureLogAt = AtomicLong(0)
        private val lastLocalCleanupAt = AtomicLong(0)
        private val localWindows = ConcurrentHashMap<String, LocalWindow>()
        private val RATE_LIMIT_SCRIPT = DefaultRedisScript(
            """
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
              redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return current
            """.trimIndent(),
            Long::class.java
        )
    }
}
