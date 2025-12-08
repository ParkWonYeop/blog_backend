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

