package me.wypark.blogbackend.domain.chess

import me.wypark.blogbackend.api.dto.ChessGameCreateRequest
import me.wypark.blogbackend.api.dto.ChessMoveRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ChessGameServiceTest {

    private lateinit var store: FakeChessGameStore
    private lateinit var historyStore: FakeChessGameHistoryStore
    private lateinit var engine: FakeMaiaEngine
    private lateinit var service: ChessGameService

    @BeforeEach
    fun setUp() {
        store = FakeChessGameStore()
        historyStore = FakeChessGameHistoryStore()
        engine = FakeMaiaEngine()
        service = ChessGameService(
            chessGameStore = store,
            chessGameHistoryStore = historyStore,
            maiaEngine = engine,
            clock = Clock.fixed(Instant.parse("2026-06-19T00:00:00Z"), ZoneOffset.UTC)
        )
    }

    @Test
    fun `creates white game without an immediate Maia move`() {
        val response = service.createGame(
            memberId = MEMBER_ID,
            request = ChessGameCreateRequest(rating = 1500, playerColor = "white", model = "5m")
        )

        assertEquals(1500, response.rating)
        assertEquals("white", response.playerColor)
        assertEquals(emptyList(), response.moves)
        assertEquals(null, response.maiaMove)
        assertEquals("white", response.turn)
        assertEquals("IN_PROGRESS", response.status)
        assertEquals("IN_PROGRESS", response.outcome)
        assertEquals(MEMBER_ID, historyStore.savedSessions.single().memberId)
    }

    @Test
    fun `creates black game with Maia opening move`() {
        engine.playResponses.add(
            MaiaPlayResponse(
                move = "e2e4",
                fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1",
                turn = "black",
                status = "IN_PROGRESS",
                result = null,
                pgn = "1. e4 *"
            )
        )

        val response = service.createGame(
            memberId = MEMBER_ID,
            request = ChessGameCreateRequest(rating = 1500, playerColor = "black", model = "5m")
        )

        assertEquals(listOf("e2e4"), response.moves)
        assertEquals("e2e4", response.maiaMove)
        assertEquals("black", response.turn)
        assertEquals("1. e4 *", response.pgn)
        assertEquals(1, engine.playRequests.size)
    }

    @Test
    fun `stores player move Maia reply PGN and outcome`() {
        val session = store.save(
            ChessGameSession(
                gameId = "game-1",
                memberId = MEMBER_ID,
                rating = 1500,
                playerColor = ChessSide.WHITE,
                model = "5m",
                temperature = 0.8,
                topP = 0.95,
                fen = FakeMaiaEngine.START_FEN,
                turn = ChessSide.WHITE,
                moves = emptyList(),
                status = "IN_PROGRESS",
                result = null,
                pgn = "*",
                createdAt = Instant.parse("2026-06-19T00:00:00Z"),
                updatedAt = Instant.parse("2026-06-19T00:00:00Z")
            )
        )
        engine.playResponses.add(
            MaiaPlayResponse(
                move = "c7c5",
                fen = "rnbqkbnr/pp1ppppp/8/2p5/4P3/8/PPPP1PPP/RNBQKBNR w KQkq c6 0 2",
                turn = "white",
                status = "IN_PROGRESS",
                result = null,
                pgn = "1. e4 c5 *"
            )
        )

        val response = service.playMove(MEMBER_ID, session.gameId, ChessMoveRequest("e2e4"))

        assertEquals(listOf("e2e4", "c7c5"), response.moves)
        assertEquals("c7c5", response.maiaMove)
        assertEquals("white", response.turn)
        assertEquals("1. e4 c5 *", response.pgn)
        assertEquals("IN_PROGRESS", response.outcome)
        assertEquals(listOf("e2e4"), engine.playRequests.single().moves)
        assertEquals("1. e4 c5 *", historyStore.savedSessions.last().pgn)
    }

    @Test
    fun `records a player win when result favors player color`() {
        val session = store.save(
            ChessGameSession(
                gameId = "game-1",
                memberId = MEMBER_ID,
                rating = 1500,
                playerColor = ChessSide.WHITE,
                model = "5m",
                temperature = 0.8,
                topP = 0.95,
                fen = FakeMaiaEngine.START_FEN,
                turn = ChessSide.WHITE,
                moves = emptyList(),
                status = "IN_PROGRESS",
                result = null,
                pgn = "*",
                createdAt = Instant.parse("2026-06-19T00:00:00Z"),
                updatedAt = Instant.parse("2026-06-19T00:00:00Z")
            )
        )
        engine.playResponses.add(
            MaiaPlayResponse(
                move = null,
                fen = "8/8/8/8/8/8/8/8 b - - 0 1",
                turn = "black",
                status = "CHECKMATE",
                result = "1-0",
                pgn = "1. e4 1-0"
            )
        )

        val response = service.playMove(MEMBER_ID, session.gameId, ChessMoveRequest("e2e4"))

        assertEquals("WIN", response.outcome)
        assertEquals("WIN", historyStore.savedSessions.last().outcome().name)
    }

    @Test
    fun `rejects move when it is not player's turn`() {
        store.save(
            ChessGameSession(
                gameId = "game-1",
                memberId = MEMBER_ID,
                rating = 1500,
                playerColor = ChessSide.WHITE,
                model = "5m",
                temperature = 0.8,
                topP = 0.95,
                fen = FakeMaiaEngine.START_FEN,
                turn = ChessSide.BLACK,
                moves = listOf("e2e4"),
                status = "IN_PROGRESS",
                result = null,
                pgn = "1. e4 *",
                createdAt = Instant.parse("2026-06-19T00:00:00Z"),
                updatedAt = Instant.parse("2026-06-19T00:00:00Z")
            )
        )

        assertFailsWith<IllegalArgumentException> {
            service.playMove(MEMBER_ID, "game-1", ChessMoveRequest("d2d4"))
        }
    }

    @Test
    fun `rejects access to another member game`() {
        store.save(
            ChessGameSession(
                gameId = "game-1",
                memberId = OTHER_MEMBER_ID,
                rating = 1500,
                playerColor = ChessSide.WHITE,
                model = "5m",
                temperature = 0.8,
                topP = 0.95,
                fen = FakeMaiaEngine.START_FEN,
                turn = ChessSide.WHITE,
                moves = emptyList(),
                status = "IN_PROGRESS",
                result = null,
                pgn = "*",
                createdAt = Instant.parse("2026-06-19T00:00:00Z"),
                updatedAt = Instant.parse("2026-06-19T00:00:00Z")
            )
        )

        assertFailsWith<IllegalArgumentException> {
            service.getGame(MEMBER_ID, "game-1")
        }
    }

    companion object {
        private const val MEMBER_ID = 1L
        private const val OTHER_MEMBER_ID = 2L
    }
}

private class FakeChessGameStore : ChessGameStore {
    private val sessions = mutableMapOf<String, ChessGameSession>()

    override fun save(session: ChessGameSession): ChessGameSession {
        sessions[session.gameId] = session
        return session
    }

    override fun findById(gameId: String): ChessGameSession? = sessions[gameId]
}

private class FakeChessGameHistoryStore : ChessGameHistoryStore {
    val savedSessions = mutableListOf<ChessGameSession>()

    override fun save(session: ChessGameSession) {
        savedSessions.add(session)
    }

    override fun findByGameIdAndMemberId(gameId: String, memberId: Long): ChessGameRecord? = null

    override fun findAllByMemberId(memberId: Long, pageable: Pageable): Page<ChessGameRecord> {
        return PageImpl(emptyList(), pageable, 0)
    }

    override fun getStats(memberId: Long): ChessGameHistoryStats {
        return ChessGameHistoryStats(
            total = 0,
            inProgress = 0,
            wins = 0,
            losses = 0,
            draws = 0,
            unknown = 0
        )
    }
}

private class FakeMaiaEngine : MaiaEngine {
    val playResponses = ArrayDeque<MaiaPlayResponse>()
    val playRequests = mutableListOf<MaiaPlayRequest>()

    override fun getState(request: MaiaStateRequest): MaiaStateResponse {
        return MaiaStateResponse(
            fen = START_FEN,
            turn = "white",
            status = "IN_PROGRESS",
            result = null,
            pgn = "*"
        )
    }

    override fun playMove(request: MaiaPlayRequest): MaiaPlayResponse {
        playRequests.add(request)
        return playResponses.removeFirst()
    }

    companion object {
        const val START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    }
}
