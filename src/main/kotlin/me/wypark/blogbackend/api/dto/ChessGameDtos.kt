package me.wypark.blogbackend.api.dto

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Pattern
import me.wypark.blogbackend.domain.chess.ChessGameHistoryStats
import me.wypark.blogbackend.domain.chess.ChessGameRecord
import me.wypark.blogbackend.domain.chess.ChessGameSession
import java.time.LocalDateTime

data class ChessGameCreateRequest(
    @field:Min(600, message = "레이팅은 600 이상이어야 합니다.")
    @field:Max(2600, message = "레이팅은 2600 이하여야 합니다.")
    val rating: Int = 1500,

    @field:Pattern(
        regexp = "(?i)^(white|black)$",
        message = "playerColor는 white 또는 black이어야 합니다."
    )
    val playerColor: String = "white",

    @field:Pattern(
        regexp = "^(3m|5m|23m|79m)$",
        message = "model은 3m, 5m, 23m, 79m 중 하나여야 합니다."
    )
    val model: String = "5m",

    @field:DecimalMin(value = "0.0", message = "temperature는 0.0 이상이어야 합니다.")
    @field:DecimalMax(value = "2.0", message = "temperature는 2.0 이하여야 합니다.")
    val temperature: Double = 0.8,

    @field:DecimalMin(value = "0.0", message = "topP는 0.0 이상이어야 합니다.")
    @field:DecimalMax(value = "1.0", message = "topP는 1.0 이하여야 합니다.")
    val topP: Double = 0.95
)

data class ChessMoveRequest(
    @field:Pattern(
        regexp = "^[a-h][1-8][a-h][1-8][qrbn]?$",
        message = "move는 UCI 형식이어야 합니다. 예: e2e4, e7e8q"
    )
    val move: String
)

data class ChessGameResponse(
    val gameId: String,
    val rating: Int,
    val playerColor: String,
    val model: String,
    val fen: String,
    val turn: String,
    val moves: List<String>,
    val status: String,
    val result: String?,
    val outcome: String,
    val pgn: String,
    val maiaMove: String?
) {
    companion object {
        fun from(session: ChessGameSession, maiaMove: String? = null): ChessGameResponse {
            return ChessGameResponse(
                gameId = session.gameId,
                rating = session.rating,
                playerColor = session.playerColor.value,
                model = session.model,
                fen = session.fen,
                turn = session.turn.value,
                moves = session.moves,
                status = session.status,
                result = session.result,
                outcome = session.outcome().name,
                pgn = session.pgn,
                maiaMove = maiaMove
            )
        }

        fun from(record: ChessGameRecord, maiaMove: String? = null): ChessGameResponse {
            return ChessGameResponse(
                gameId = record.gameId,
                rating = record.rating,
                playerColor = record.playerColor.value,
                model = record.model,
                fen = record.fen,
                turn = record.turn.value,
                moves = record.moveList(),
                status = record.status,
                result = record.result,
                outcome = record.outcome.name,
                pgn = record.pgn,
                maiaMove = maiaMove
            )
        }
    }
}

data class ChessGameSummaryResponse(
    val gameId: String,
    val rating: Int,
    val playerColor: String,
    val model: String,
    val status: String,
    val result: String?,
    val outcome: String,
    val movesCount: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(record: ChessGameRecord): ChessGameSummaryResponse {
            return ChessGameSummaryResponse(
                gameId = record.gameId,
                rating = record.rating,
                playerColor = record.playerColor.value,
                model = record.model,
                status = record.status,
                result = record.result,
                outcome = record.outcome.name,
                movesCount = record.moveList().size,
                createdAt = record.createdAt,
                updatedAt = record.updatedAt
            )
        }
    }
}

data class ChessGamePgnResponse(
    val gameId: String,
    val pgn: String
)

data class ChessGameStatsResponse(
    val total: Long,
    val inProgress: Long,
    val wins: Long,
    val losses: Long,
    val draws: Long,
    val unknown: Long
) {
    companion object {
        fun from(stats: ChessGameHistoryStats): ChessGameStatsResponse {
            return ChessGameStatsResponse(
                total = stats.total,
                inProgress = stats.inProgress,
                wins = stats.wins,
                losses = stats.losses,
                draws = stats.draws,
                unknown = stats.unknown
            )
        }
    }
}
