package me.wypark.blogbackend.domain.auth.infra

import me.wypark.blogbackend.domain.auth.service.RefreshTokenStore
import me.wypark.blogbackend.global.config.JwtProperties
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

@Repository
class RedisRefreshTokenStore(
    private val redisTemplate: RedisTemplate<String, String>,
    private val jwtProperties: JwtProperties
) : RefreshTokenStore {

    override fun save(email: String, refreshToken: String) {
        redisTemplate.opsForValue().set(
            key(email),
            hash(refreshToken),
            jwtProperties.refreshTokenValidity,
            TimeUnit.MILLISECONDS
        )
    }

    override fun matches(email: String, refreshToken: String): Boolean {
        val storedHash = redisTemplate.opsForValue().get(key(email)) ?: return false
        return MessageDigest.isEqual(
            storedHash.toByteArray(StandardCharsets.US_ASCII),
            hash(refreshToken).toByteArray(StandardCharsets.US_ASCII)
        )
    }

    override fun delete(email: String) {
        redisTemplate.delete(key(email))
    }

    private fun key(email: String): String = "$KEY_PREFIX${hash(email.lowercase())}"

    private fun hash(token: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(token.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val KEY_PREFIX = "RT:"
    }
}
