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

        val initialState = maiaEngine.getState(
            MaiaStateRequest(
                moves = emptyList(),
                white = labels.white,
                black = labels.black
            )
        )
        var session = ChessGameSession(
            gameId = UUID.randomUUID().toString(),
            memberId = memberId,
            rating = request.rating,
            playerColor = playerColor,
            model = request.model,
            temperature = request.temperature,
            topP = request.topP,
            fen = initialState.fen,
            turn = ChessSide.from(initialState.turn),
            moves = emptyList(),
            status = initialState.status,
            result = initialState.result,
            pgn = initialState.pgn,
            createdAt = now,
            updatedAt = now
        )

        var maiaMove: String? = null
        if (playerColor == ChessSide.BLACK) {
            val maiaResponse = maiaEngine.playMove(session.toMaiaPlayRequest())
            maiaMove = maiaResponse.move
            session = session.applyEngineResponse(maiaResponse, now)
        }

        saveSession(session)
        return ChessGameResponse.from(session, maiaMove)
    }

    fun getGames(memberId: Long, pageable: Pageable): Page<ChessGameSummaryResponse> {
        return chessGameHistoryStore.findAllByMemberId(memberId, pageable)
            .map { ChessGameSummaryResponse.from(it) }
    }

    fun getStats(memberId: Long): ChessGameStatsResponse {
        return ChessGameStatsResponse.from(chessGameHistoryStore.getStats(memberId))
    }
