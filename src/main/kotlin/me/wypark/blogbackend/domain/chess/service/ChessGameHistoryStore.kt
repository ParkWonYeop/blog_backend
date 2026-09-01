package me.wypark.blogbackend.domain.chess.service

import me.wypark.blogbackend.domain.chess.entity.ChessGameRecord
import me.wypark.blogbackend.domain.chess.entity.ChessGameSession
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface ChessGameHistoryStore {

    fun save(session: ChessGameSession)

    fun findByGameIdAndMemberId(gameId: String, memberId: Long): ChessGameRecord?

    fun findAllByMemberId(memberId: Long, pageable: Pageable): Page<ChessGameRecord>

    fun getStats(memberId: Long): ChessGameHistoryStats
}

data class ChessGameHistoryStats(
    val total: Long,
    val inProgress: Long,
    val wins: Long,
    val losses: Long,
    val draws: Long,
    val unknown: Long
)
