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

