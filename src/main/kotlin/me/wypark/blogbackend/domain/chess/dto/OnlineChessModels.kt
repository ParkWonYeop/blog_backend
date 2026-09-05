package me.wypark.blogbackend.domain.chess.dto

import me.wypark.blogbackend.domain.chess.entity.ChessGameOutcome
import me.wypark.blogbackend.domain.chess.entity.ChessSide
import me.wypark.blogbackend.domain.chess.entity.OnlineGame
import me.wypark.blogbackend.domain.chess.entity.OnlinePlayer
import me.wypark.blogbackend.domain.chess.entity.TimeControl
import java.time.Instant

data class TimeControlResponse(
    val key: String,
    val label: String,
    val initialSeconds: Int,
    val incrementSeconds: Int
) {
    companion object {
        fun from(timeControl: TimeControl): TimeControlResponse {
            return TimeControlResponse(
                key = timeControl.name,
                label = timeControl.label,
                initialSeconds = timeControl.initialSeconds,
                incrementSeconds = timeControl.incrementSeconds
            )
        }
    }
}

data class OnlinePlayerResponse(
    val memberId: Long,
    val nickname: String,
    val connected: Boolean
) {
    companion object {
        fun from(player: OnlinePlayer, connected: Boolean): OnlinePlayerResponse {
            return OnlinePlayerResponse(player.memberId, player.nickname, connected)
        }
    }
}

data class OnlineGameResponse(
    val gameId: String,
    val timeControl: TimeControlResponse,
    val white: OnlinePlayerResponse,
    val black: OnlinePlayerResponse,
    val moves: List<String>,
    val fen: String,
    val turn: String,
    val status: String,
    val result: String?,
    val pgn: String,
    /** 응답 시점(serverTime) 기준 남은 시간. 차례인 쪽은 클라이언트가 serverTime부터 계속 줄여서 표시한다. */
    val whiteMillis: Long,
    val blackMillis: Long,
    val clockRunning: Boolean,
    val drawOfferedBy: String?,
    val forfeitDeadlineAt: Long?,
    val serverTime: Long,
    val createdAt: Long,
    val finishedAt: Long?
) {
    companion object {
        fun from(game: OnlineGame, now: Instant): OnlineGameResponse {
            return OnlineGameResponse(
                gameId = game.gameId,
                timeControl = TimeControlResponse.from(game.timeControl),
                white = OnlinePlayerResponse.from(game.white, ChessSide.WHITE !in game.disconnected),
                black = OnlinePlayerResponse.from(game.black, ChessSide.BLACK !in game.disconnected),
                moves = game.moves,
                fen = game.fen,
                turn = game.turn.value,
                status = game.status,
                result = game.result,
                pgn = game.pgn,
                whiteMillis = game.remainingMillis(ChessSide.WHITE, now),
                blackMillis = game.remainingMillis(ChessSide.BLACK, now),
                clockRunning = game.clockRunning,
                drawOfferedBy = game.drawOfferedBy?.value,
                forfeitDeadlineAt = game.forfeitDeadlineAt?.toEpochMilli(),
                serverTime = now.toEpochMilli(),
                createdAt = game.createdAt.toEpochMilli(),
                finishedAt = game.finishedAt?.toEpochMilli()
            )
        }
    }
}

/** 기록 목록용 요약. myColor·outcome은 요청한 회원 기준이다. */
data class OnlineGameSummaryResponse(
    val gameId: String,
    val timeControl: TimeControlResponse,
    val white: String,
    val black: String,
    val myColor: String,
    val opponent: String,
    val status: String,
    val result: String?,
    val outcome: String,
    val movesCount: Int,
    val startedAt: Long,
    val finishedAt: Long?
) {
    companion object {
        fun from(game: OnlineGame, memberId: Long): OnlineGameSummaryResponse {
            val myColor = game.sideOf(memberId) ?: ChessSide.WHITE
            return OnlineGameSummaryResponse(
                gameId = game.gameId,
                timeControl = TimeControlResponse.from(game.timeControl),
                white = game.white.nickname,
                black = game.black.nickname,
                myColor = myColor.value,
                opponent = game.player(myColor.opposite()).nickname,
                status = game.status,
                result = game.result,
                outcome = ChessGameOutcome.from(result = game.result, playerColor = myColor, status = game.status).name,
                movesCount = game.moves.size,
                startedAt = game.createdAt.toEpochMilli(),
                finishedAt = game.finishedAt?.toEpochMilli()
            )
        }
    }
}

/** 클라이언트 → 서버 WebSocket 메시지. type에 따라 쓰이는 필드만 채운다. */
data class OnlineClientMessage(
    val type: String,
    val token: String? = null,
    val timeControl: String? = null,
    val code: String? = null,
    val gameId: String? = null,
    val move: String? = null
)

data class AuthOkMessage(val memberId: Long, val nickname: String) {
    val type: String = "AUTH_OK"
}

data class ErrorMessage(val message: String, val code: String = "ERROR") {
    val type: String = "ERROR"
}

data class InviteCreatedMessage(val code: String, val timeControl: TimeControlResponse) {
    val type: String = "INVITE_CREATED"
}

data class QueueJoinedMessage(val timeControl: TimeControlResponse) {
    val type: String = "QUEUE_JOINED"
}

data class MatchFoundMessage(val gameId: String) {
    val type: String = "MATCH_FOUND"
}

data class GameStateMessage(val game: OnlineGameResponse) {
    val type: String = "GAME_STATE"
}

data class SimpleMessage(val type: String)
