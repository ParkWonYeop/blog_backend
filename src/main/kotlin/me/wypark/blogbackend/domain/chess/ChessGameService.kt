package me.wypark.blogbackend.domain.chess

import me.wypark.blogbackend.api.dto.ChessGameCreateRequest
import me.wypark.blogbackend.api.dto.ChessGamePgnResponse
import me.wypark.blogbackend.api.dto.ChessGameResponse
import me.wypark.blogbackend.api.dto.ChessGameStatsResponse
import me.wypark.blogbackend.api.dto.ChessGameSummaryResponse
import me.wypark.blogbackend.api.dto.ChessMoveRequest
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

    fun getGame(memberId: Long, gameId: String): ChessGameResponse {
        val session = chessGameStore.findById(gameId)
        if (session != null) {
            requireOwnedBy(session, memberId)
            return ChessGameResponse.from(session)
        }

        val record = chessGameHistoryStore.findByGameIdAndMemberId(gameId, memberId)
            ?: throw IllegalArgumentException("Chess game not found.")
        return ChessGameResponse.from(record)
    }

    fun getPgn(memberId: Long, gameId: String): ChessGamePgnResponse {
        val game = getGame(memberId, gameId)
        return ChessGamePgnResponse(gameId = game.gameId, pgn = game.pgn)
    }

    @Transactional
    fun playMove(memberId: Long, gameId: String, request: ChessMoveRequest): ChessGameResponse {
        val session = getPlayableSession(memberId, gameId)
        require(session.status == "IN_PROGRESS") { "This chess game is already finished." }
        require(session.turn == session.playerColor) { "It is not the player's turn." }

        val movesAfterPlayer = session.moves + request.move
        val maiaResponse = maiaEngine.playMove(
            session.toMaiaPlayRequest(moves = movesAfterPlayer)
        )

        val updated = session.applyEngineResponse(maiaResponse, Instant.now(clock), movesAfterPlayer)
        saveSession(updated)
        return ChessGameResponse.from(updated, maiaResponse.move)
    }

    private fun getPlayableSession(memberId: Long, gameId: String): ChessGameSession {
        val session = chessGameStore.findById(gameId)
        if (session != null) {
            requireOwnedBy(session, memberId)
            return session
        }

        val record = chessGameHistoryStore.findByGameIdAndMemberId(gameId, memberId)
            ?: throw IllegalArgumentException("Chess game not found.")
        return record.toSession()
    }

    private fun saveSession(session: ChessGameSession) {
        chessGameStore.save(session)
        chessGameHistoryStore.save(session)
    }

    private fun requireOwnedBy(session: ChessGameSession, memberId: Long) {
        require(session.memberId == memberId) { "Chess game not found." }
    }

    private fun playerLabels(playerColor: ChessSide, rating: Int, model: String): PlayerLabels {
        val maiaName = "Maia3-$model-$rating"
        return if (playerColor == ChessSide.WHITE) {
            PlayerLabels(white = "Player", black = maiaName)
        } else {
            PlayerLabels(white = maiaName, black = "Player")
        }
    }

    private fun ChessGameSession.toMaiaPlayRequest(
        moves: List<String> = this.moves
    ): MaiaPlayRequest {
        val labels = playerLabels(playerColor, rating, model)
        return MaiaPlayRequest(
            moves = moves,
            rating = rating,
            model = model,
            temperature = temperature,
            topP = topP,
            white = labels.white,
            black = labels.black
        )
    }

    private fun ChessGameSession.applyEngineResponse(
        response: MaiaPlayResponse,
        now: Instant,
        movesBeforeMaia: List<String> = moves
    ): ChessGameSession {
        val nextMoves = response.move?.let { movesBeforeMaia + it } ?: movesBeforeMaia
        return copy(
            fen = response.fen,
            turn = ChessSide.from(response.turn),
            moves = nextMoves,
            status = response.status,
            result = response.result,
            pgn = response.pgn,
            updatedAt = now
        )
    }

    private data class PlayerLabels(
        val white: String,
        val black: String
    )
}
