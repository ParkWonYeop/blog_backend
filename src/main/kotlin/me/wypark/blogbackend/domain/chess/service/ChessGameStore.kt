package me.wypark.blogbackend.domain.chess.service

import me.wypark.blogbackend.domain.chess.entity.ChessGameSession

interface ChessGameStore {

    fun save(session: ChessGameSession): ChessGameSession

    fun findById(gameId: String): ChessGameSession?
}
