package me.wypark.blogbackend.domain.chess

import com.fasterxml.jackson.databind.ObjectMapper
import me.wypark.blogbackend.core.config.MaiaProperties
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository
import java.util.concurrent.TimeUnit

@Repository
class RedisChessGameStore(
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
    private val maiaProperties: MaiaProperties
) : ChessGameStore {

    override fun save(session: ChessGameSession): ChessGameSession {
        redisTemplate.opsForValue().set(
            key(session.gameId),
            objectMapper.writeValueAsString(session),
            maiaProperties.gameSessionTtl.toMillis(),
            TimeUnit.MILLISECONDS
        )
        return session
    }

    override fun findById(gameId: String): ChessGameSession? {
        val json = redisTemplate.opsForValue().get(key(gameId)) ?: return null
        return objectMapper.readValue(json, ChessGameSession::class.java)
    }

    private fun key(gameId: String): String = "CHESS_GAME:$gameId"
}
