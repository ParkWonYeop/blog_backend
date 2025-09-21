package me.wypark.blogbackend.application.chess

import me.wypark.blogbackend.domain.chess.ChessGameSession

interface ChessGameStore {

    fun save(session: ChessGameSession): ChessGameSession

    fun findById(gameId: String): ChessGameSession?
}
