package me.wypark.blogbackend.domain.chess.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import me.wypark.blogbackend.global.common.BaseTimeEntity
import java.time.Instant

@Entity
@Table(
    name = "chess_online_game",
    uniqueConstraints = [UniqueConstraint(name = "uk_chess_online_game_game_id", columnNames = ["game_id"])],
    indexes = [
        Index(name = "idx_chess_online_game_white", columnList = "white_member_id, finished_at"),
        Index(name = "idx_chess_online_game_black", columnList = "black_member_id, finished_at")
    ]
)
class OnlineGameRecord(
    @Column(name = "game_id", nullable = false, length = 36)
    val gameId: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "time_control", nullable = false, length = 20)
    val timeControl: TimeControl,

    @Column(name = "white_member_id", nullable = false)
    val whiteMemberId: Long,

    @Column(name = "white_nickname", nullable = false, length = 100)
    val whiteNickname: String,

    @Column(name = "black_member_id", nullable = false)
    val blackMemberId: Long,

    @Column(name = "black_nickname", nullable = false, length = 100)
    val blackNickname: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var moves: String,

    @Column(nullable = false, length = 40)
    var status: String,

    @Column(length = 16)
    var result: String?,

    @Column(nullable = false, columnDefinition = "TEXT")
    var pgn: String,

    @Column(name = "white_millis", nullable = false)
    var whiteMillis: Long,

    @Column(name = "black_millis", nullable = false)
    var blackMillis: Long,

    @Column(name = "started_at", nullable = false)
    val startedAt: Instant,

    @Column(name = "finished_at")
    var finishedAt: Instant?
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    fun apply(game: OnlineGame) {
        moves = game.moves.joinToString(" ")
        status = game.status
        result = game.result
        pgn = game.pgn
        whiteMillis = game.whiteMillis
        blackMillis = game.blackMillis
        finishedAt = game.finishedAt
    }

    fun toGame(): OnlineGame {
        return OnlineGame(
            gameId = gameId,
            timeControl = timeControl,
            white = OnlinePlayer(whiteMemberId, whiteNickname),
            black = OnlinePlayer(blackMemberId, blackNickname),
            moves = moves.split(" ").filter { it.isNotBlank() },
            fen = "",
            turn = if (moves.split(" ").count { it.isNotBlank() } % 2 == 0) ChessSide.WHITE else ChessSide.BLACK,
            status = status,
            result = result,
            pgn = pgn,
            whiteMillis = whiteMillis,
            blackMillis = blackMillis,
            lastMoveAt = null,
            drawOfferedBy = null,
            disconnected = emptySet(),
            forfeitDeadlineAt = null,
            createdAt = startedAt,
            finishedAt = finishedAt
        )
    }

    companion object {
        fun from(game: OnlineGame): OnlineGameRecord {
            return OnlineGameRecord(
                gameId = game.gameId,
                timeControl = game.timeControl,
                whiteMemberId = game.white.memberId,
                whiteNickname = game.white.nickname,
                blackMemberId = game.black.memberId,
                blackNickname = game.black.nickname,
                moves = game.moves.joinToString(" "),
                status = game.status,
                result = game.result,
                pgn = game.pgn,
                whiteMillis = game.whiteMillis,
                blackMillis = game.blackMillis,
                startedAt = game.createdAt,
                finishedAt = game.finishedAt
            )
        }
    }
}
