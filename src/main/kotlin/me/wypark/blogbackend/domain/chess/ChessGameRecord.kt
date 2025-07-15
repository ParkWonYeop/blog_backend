package me.wypark.blogbackend.domain.chess

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import me.wypark.blogbackend.domain.common.BaseTimeEntity
import me.wypark.blogbackend.domain.user.Member
import java.time.ZoneId

@Entity
@Table(
    name = "chess_game_record",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_chess_game_record_game_id", columnNames = ["game_id"])
    ],
    indexes = [
        Index(name = "idx_chess_game_record_member_updated_at", columnList = "member_id, updated_at"),
        Index(name = "idx_chess_game_record_member_outcome", columnList = "member_id, outcome")
    ]
)
class ChessGameRecord(
    @Column(name = "game_id", nullable = false, length = 36)
    val gameId: String,

