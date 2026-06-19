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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    val member: Member,

    @Column(nullable = false)
    var rating: Int,

    @Enumerated(EnumType.STRING)
    @Column(name = "player_color", nullable = false, length = 10)
    var playerColor: ChessSide,

    @Column(nullable = false, length = 10)
    var model: String,

    @Column(nullable = false)
    var temperature: Double,

    @Column(name = "top_p", nullable = false)
    var topP: Double,

    @Column(nullable = false, columnDefinition = "TEXT")
    var fen: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    var turn: ChessSide,

    @Column(nullable = false, columnDefinition = "TEXT")
    var moves: String,

    @Column(nullable = false, length = 40)
    var status: String,

    @Column(length = 16)
    var result: String?,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var outcome: ChessGameOutcome,

    @Column(nullable = false, columnDefinition = "TEXT")
    var pgn: String
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    fun apply(session: ChessGameSession) {
        rating = session.rating
        playerColor = session.playerColor
        model = session.model
        temperature = session.temperature
        topP = session.topP
        fen = session.fen
        turn = session.turn
        moves = session.moves.joinToString(" ")
        status = session.status
        result = session.result
        outcome = session.outcome()
        pgn = session.pgn
    }

    fun moveList(): List<String> {
        return moves.split(" ").filter { it.isNotBlank() }
    }

    fun toSession(): ChessGameSession {
        val zoneId = ZoneId.systemDefault()
        val memberId = requireNotNull(member.id) { "member id is required." }
        return ChessGameSession(
            gameId = gameId,
            memberId = memberId,
            rating = rating,
            playerColor = playerColor,
            model = model,
            temperature = temperature,
            topP = topP,
            fen = fen,
            turn = turn,
            moves = moveList(),
            status = status,
            result = result,
            pgn = pgn,
            createdAt = createdAt.atZone(zoneId).toInstant(),
            updatedAt = updatedAt.atZone(zoneId).toInstant()
        )
    }

    companion object {
        fun from(member: Member, session: ChessGameSession): ChessGameRecord {
            return ChessGameRecord(
                gameId = session.gameId,
                member = member,
                rating = session.rating,
                playerColor = session.playerColor,
                model = session.model,
                temperature = session.temperature,
                topP = session.topP,
                fen = session.fen,
                turn = session.turn,
                moves = session.moves.joinToString(" "),
                status = session.status,
                result = session.result,
                outcome = session.outcome(),
                pgn = session.pgn
            )
        }
    }
}
