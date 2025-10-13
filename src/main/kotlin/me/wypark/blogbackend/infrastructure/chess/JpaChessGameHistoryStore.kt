package me.wypark.blogbackend.infrastructure.chess

import me.wypark.blogbackend.application.chess.ChessGameHistoryStats
import me.wypark.blogbackend.application.chess.ChessGameHistoryStore
import me.wypark.blogbackend.domain.chess.ChessGameOutcome
import me.wypark.blogbackend.domain.chess.ChessGameRecord
import me.wypark.blogbackend.domain.chess.ChessGameRecordRepository
import me.wypark.blogbackend.domain.chess.ChessGameSession
import me.wypark.blogbackend.domain.user.MemberRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class JpaChessGameHistoryStore(
    private val chessGameRecordRepository: ChessGameRecordRepository,
    private val memberRepository: MemberRepository
) : ChessGameHistoryStore {

    @Transactional
    override fun save(session: ChessGameSession) {
        val existing = chessGameRecordRepository.findByGameId(session.gameId)
        if (existing != null) {
            require(existing.member.id == session.memberId) { "Chess game not found." }
            existing.apply(session)
            return
        }
