package me.wypark.blogbackend.domain.chess.repository
import me.wypark.blogbackend.domain.chess.entity.ChessGameOutcome
import me.wypark.blogbackend.domain.chess.entity.ChessGameRecord

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface ChessGameRecordRepository : JpaRepository<ChessGameRecord, Long> {

    fun findByGameId(gameId: String): ChessGameRecord?

    fun findByGameIdAndMember_Id(gameId: String, memberId: Long): ChessGameRecord?

    fun findAllByMember_Id(memberId: Long, pageable: Pageable): Page<ChessGameRecord>

    fun countByMember_Id(memberId: Long): Long

    fun countByMember_IdAndOutcome(memberId: Long, outcome: ChessGameOutcome): Long
}
