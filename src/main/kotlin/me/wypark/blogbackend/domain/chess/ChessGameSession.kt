package me.wypark.blogbackend.domain.chess

import java.time.Instant

data class ChessGameSession(
    val gameId: String,
    val memberId: Long,
    val rating: Int,
    val playerColor: ChessSide,
    val model: String,
    val temperature: Double,
    val topP: Double,
    val fen: String,
    val turn: ChessSide,
    val moves: List<String>,
    val status: String,
    val result: String?,
    val pgn: String,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    fun outcome(): ChessGameOutcome {
        return ChessGameOutcome.from(result = result, playerColor = playerColor, status = status)
    }
}

enum class ChessSide(val value: String) {
    WHITE("white"),
    BLACK("black");

    fun opposite(): ChessSide = if (this == WHITE) BLACK else WHITE

    companion object {
        fun from(value: String): ChessSide {
            return entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("playerColor must be white or black.")
        }
    }
}

enum class ChessGameOutcome {
    IN_PROGRESS,
    WIN,
    LOSS,
    DRAW,
    UNKNOWN;

    companion object {
        fun from(result: String?, playerColor: ChessSide, status: String): ChessGameOutcome {
            if (status == "IN_PROGRESS") {
                return IN_PROGRESS
            }

            return when (result) {
                "1-0" -> if (playerColor == ChessSide.WHITE) WIN else LOSS
                "0-1" -> if (playerColor == ChessSide.BLACK) WIN else LOSS
                "1/2-1/2" -> DRAW
                null, "", "*" -> UNKNOWN
                else -> UNKNOWN
            }
        }
    }
}
