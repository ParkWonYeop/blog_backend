package me.wypark.blogbackend.api.dto

import me.wypark.blogbackend.domain.chess.ChessPuzzle
import java.time.LocalDate

data class ChessPuzzleResponse(
    val id: Long,
    val date: LocalDate,
    val title: String,
    val theme: String,
    val fen: String,
    val answer: String,
    val answerUci: String,
    val hint: String,
    val rating: Int,
    val sourceUrl: String
) {
    companion object {
        fun from(puzzle: ChessPuzzle, date: LocalDate): ChessPuzzleResponse {
            return ChessPuzzleResponse(
                id = puzzle.id!!,
                date = date,
                title = puzzle.title,
                theme = puzzle.theme,
                fen = puzzle.fen,
                answer = puzzle.answer,
                answerUci = puzzle.answerUci,
                hint = puzzle.hint,
                rating = puzzle.rating,
                sourceUrl = puzzle.sourceUrl
            )
        }
    }
}
