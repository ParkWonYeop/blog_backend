package me.wypark.blogbackend.domain.chess.repository

import me.wypark.blogbackend.domain.chess.entity.OnlineGameRecord
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface OnlineGameRecordRepository : JpaRepository<OnlineGameRecord, Long> {
    fun findByGameId(gameId: String): OnlineGameRecord?

    fun findAllByWhiteMemberIdOrBlackMemberId(whiteMemberId: Long, blackMemberId: Long, pageable: Pageable): Page<OnlineGameRecord>
}
