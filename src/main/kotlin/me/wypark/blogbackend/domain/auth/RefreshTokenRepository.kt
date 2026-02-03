package me.wypark.blogbackend.domain.auth

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository
import java.util.concurrent.TimeUnit

/**
 * [Refresh Token 저장소]
 *
 * JWT 인증 방식의 핵심인 Refresh Token의 생명주기(저장, 조회, 삭제)를 관리하는 리포지토리입니다.
 *
 * [기술적 의사결정: Redis]
 * RDB 대신 In-Memory DB인 Redis를 선택한 이유는 다음과 같습니다.
 * 1. TTL(Time-To-Live): 토큰 만료 시 별도의 배치 작업 없이 자동으로 데이터를 삭제하여 스토리지 공간을 효율적으로 관리할 수 있습니다.
 * 2. Performance: 잦은 I/O가 발생하는 토큰 검증 과정에서 디스크 기반 DB보다 훨씬 빠른 응답 속도를 보장합니다.
 */
@Repository
class RefreshTokenRepository(
    private val redisTemplate: RedisTemplate<String, String>,
    @Value("\${jwt.refresh-token-validity}") private val refreshTokenValidity: Long
) {

    /**
     * Refresh Token을 저장합니다.
     *
     * @param email 사용자 식별자 (Key)
     * @param refreshToken 발급된 토큰 (Value)
     *
     * Key에는 "RT:" 접두어(Prefix)를 붙여 Redis 내의 다른 데이터와 네임스페이스를 분리합니다.
     * 유효 기간(refreshTokenValidity)을 설정하여 해당 시간이 지나면 Redis에서 자동 소멸되도록 합니다.
     */
    fun save(email: String, refreshToken: String) {
        redisTemplate.opsForValue().set(
            "RT:$email",
            refreshToken,
            refreshTokenValidity,
            TimeUnit.MILLISECONDS
        )
    }

    /**
     * 사용자의 이메일로 저장된 Refresh Token을 조회합니다.
     *
     * 토큰 재발급(Reissue) 요청 시 클라이언트가 보낸 토큰과 서버에 저장된 토큰의 일치 여부를
     * 검증하기 위해 사용됩니다. (Refresh Token Rotation 전략의 핵심)
     */
    fun findByEmail(email: String): String? {
        return redisTemplate.opsForValue().get("RT:$email")
    }

    /**
     * Refresh Token을 삭제합니다.
     *
     * 사용자가 로그아웃하거나, 보안상의 이유로 토큰을 무효화해야 할 때 호출됩니다.
     * Redis에서 즉시 제거(Evict)하므로, 이후 해당 토큰으로는 액세스 토큰을 재발급받을 수 없습니다.
     */
    fun delete(email: String) {
        redisTemplate.delete("RT:$email")
    }
}