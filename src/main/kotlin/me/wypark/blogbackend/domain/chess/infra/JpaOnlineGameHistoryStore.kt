package me.wypark.blogbackend.domain.chess.infra

import me.wypark.blogbackend.domain.chess.entity.OnlineGame
import me.wypark.blogbackend.domain.chess.entity.OnlineGameRecord
import me.wypark.blogbackend.domain.chess.repository.OnlineGameRecordRepository
import me.wypark.blogbackend.domain.chess.service.OnlineGameHistoryStore
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class JpaOnlineGameHistoryStore(
    private val repository: OnlineGameRecordRepository
) : OnlineGameHistoryStore {

    @Transactional
    override fun save(game: OnlineGame) {
        val existing = repository.findByGameId(game.gameId)
        if (existing != null) {
            existing.apply(game)
            return
        }
        repository.save(OnlineGameRecord.from(game))
    }

    @Transactional(readOnly = true)
    override fun find(gameId: String): OnlineGame? {
        return repository.findByGameId(gameId)?.toGame()
    }

    @Transactional(readOnly = true)
    override fun findAllByMemberId(memberId: Long, pageable: Pageable): Page<OnlineGame> {
        return repository.findAllByWhiteMemberIdOrBlackMemberId(memberId, memberId, pageable).map { it.toGame() }
    }
}
