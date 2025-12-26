package me.wypark.blogbackend.domain.auth

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository
import java.util.concurrent.TimeUnit

@Repository
class RefreshTokenRepository(
    private val redisTemplate: RedisTemplate<String, String>,
    @Value("\${jwt.refresh-token-validity}") private val refreshTokenValidity: Long
) {
    // 저장 (Key: Email, Value: RefreshToken)
    fun save(email: String, refreshToken: String) {
        redisTemplate.opsForValue().set(
            "RT:$email",
            refreshToken,
            refreshTokenValidity,
            TimeUnit.MILLISECONDS
        )
    }

    // 조회
    fun findByEmail(email: String): String? {
        return redisTemplate.opsForValue().get("RT:$email")
    }

    // 삭제 (로그아웃 시)
    fun delete(email: String) {
        redisTemplate.delete("RT:$email")
    }
}