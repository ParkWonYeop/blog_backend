package me.wypark.blogbackend.infrastructure.auth

import me.wypark.blogbackend.application.auth.RefreshTokenStore
import me.wypark.blogbackend.core.config.JwtProperties
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository
import java.util.concurrent.TimeUnit

@Repository
class RedisRefreshTokenStore(
    private val redisTemplate: RedisTemplate<String, String>,
    private val jwtProperties: JwtProperties
) : RefreshTokenStore {

    override fun save(email: String, refreshToken: String) {
        redisTemplate.opsForValue().set(
            key(email),
            refreshToken,
            jwtProperties.refreshTokenValidity,
            TimeUnit.MILLISECONDS
        )
    }

    override fun findByEmail(email: String): String? {
        return redisTemplate.opsForValue().get(key(email))
    }

    override fun delete(email: String) {
        redisTemplate.delete(key(email))
    }

    private fun key(email: String): String = "$KEY_PREFIX$email"

    companion object {
        private const val KEY_PREFIX = "RT:"
    }
}
