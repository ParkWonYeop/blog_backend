package me.wypark.blogbackend.domain.chess

interface ChessGameStore {

    fun save(session: ChessGameSession): ChessGameSession

    fun findById(gameId: String): ChessGameSession?
}
