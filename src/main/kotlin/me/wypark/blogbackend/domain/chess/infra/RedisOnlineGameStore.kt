package me.wypark.blogbackend.domain.chess.infra

import com.fasterxml.jackson.databind.ObjectMapper
import me.wypark.blogbackend.domain.chess.entity.OnlineGame
import me.wypark.blogbackend.domain.chess.entity.OnlineInvite
import me.wypark.blogbackend.domain.chess.service.OnlineGameStore
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository
import java.time.Duration

@Repository
class RedisOnlineGameStore(
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper
) : OnlineGameStore {

    override fun save(game: OnlineGame) {
        redisTemplate.opsForValue().set(gameKey(game.gameId), objectMapper.writeValueAsString(game), GAME_TTL)
        if (game.isActive) {
            redisTemplate.opsForSet().add(ACTIVE_KEY, game.gameId)
        } else {
            redisTemplate.opsForSet().remove(ACTIVE_KEY, game.gameId)
        }
    }

    override fun find(gameId: String): OnlineGame? {
        val json = redisTemplate.opsForValue().get(gameKey(gameId)) ?: return null
        return objectMapper.readValue(json, OnlineGame::class.java)
    }

    override fun activeGameIds(): Set<String> {
        return redisTemplate.opsForSet().members(ACTIVE_KEY) ?: emptySet()
    }

    override fun saveInvite(invite: OnlineInvite) {
        redisTemplate.opsForValue().set(inviteKey(invite.code), objectMapper.writeValueAsString(invite), INVITE_TTL)
    }

    override fun findInvite(code: String): OnlineInvite? {
        val json = redisTemplate.opsForValue().get(inviteKey(code)) ?: return null
        return objectMapper.readValue(json, OnlineInvite::class.java)
    }

    override fun deleteInvite(code: String) {
        redisTemplate.delete(inviteKey(code))
    }

    private fun gameKey(gameId: String) = "CHESS_ONLINE_GAME:$gameId"

    private fun inviteKey(code: String) = "CHESS_ONLINE_INVITE:$code"

    companion object {
        private const val ACTIVE_KEY = "CHESS_ONLINE_ACTIVE"
        private val GAME_TTL: Duration = Duration.ofHours(48)
        private val INVITE_TTL: Duration = Duration.ofMinutes(30)
    }
}
