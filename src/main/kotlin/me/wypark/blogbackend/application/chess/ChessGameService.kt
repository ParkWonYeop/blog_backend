package me.wypark.blogbackend.application.chess

import me.wypark.blogbackend.domain.chess.ChessGameSession
import me.wypark.blogbackend.domain.chess.ChessSide
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
@Transactional(readOnly = true)
class ChessGameService(
    private val chessGameStore: ChessGameStore,
    private val chessGameHistoryStore: ChessGameHistoryStore,
    private val maiaEngine: MaiaEngine,
    private val clock: Clock
) {

    @Transactional
    fun createGame(memberId: Long, request: ChessGameCreateRequest): ChessGameResponse {
        val playerColor = ChessSide.from(request.playerColor)
        val now = Instant.now(clock)
        val labels = playerLabels(playerColor, request.rating, request.model)

