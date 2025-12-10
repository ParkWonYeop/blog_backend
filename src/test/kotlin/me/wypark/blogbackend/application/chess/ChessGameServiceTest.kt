package me.wypark.blogbackend.application.chess

import me.wypark.blogbackend.domain.chess.ChessGameRecord
import me.wypark.blogbackend.domain.chess.ChessGameSession
import me.wypark.blogbackend.domain.chess.ChessSide
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
