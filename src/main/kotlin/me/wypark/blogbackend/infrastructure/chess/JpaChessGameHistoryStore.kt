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

        val member = memberRepository.getReferenceById(session.memberId)
        chessGameRecordRepository.save(ChessGameRecord.from(member, session))
    }

    @Transactional(readOnly = true)
    override fun findByGameIdAndMemberId(gameId: String, memberId: Long): ChessGameRecord? {
        return chessGameRecordRepository.findByGameIdAndMember_Id(gameId, memberId)
    }

    @Transactional(readOnly = true)
    override fun findAllByMemberId(memberId: Long, pageable: Pageable): Page<ChessGameRecord> {
        return chessGameRecordRepository.findAllByMember_Id(memberId, pageable)
    }

    @Transactional(readOnly = true)
    override fun getStats(memberId: Long): ChessGameHistoryStats {
        return ChessGameHistoryStats(
            total = chessGameRecordRepository.countByMember_Id(memberId),
            inProgress = chessGameRecordRepository.countByMember_IdAndOutcome(memberId, ChessGameOutcome.IN_PROGRESS),
            wins = chessGameRecordRepository.countByMember_IdAndOutcome(memberId, ChessGameOutcome.WIN),
            losses = chessGameRecordRepository.countByMember_IdAndOutcome(memberId, ChessGameOutcome.LOSS),
            draws = chessGameRecordRepository.countByMember_IdAndOutcome(memberId, ChessGameOutcome.DRAW),
            unknown = chessGameRecordRepository.countByMember_IdAndOutcome(memberId, ChessGameOutcome.UNKNOWN)
        )
    }
}
