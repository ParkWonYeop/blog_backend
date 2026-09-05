package me.wypark.blogbackend.domain.chess.service

import me.wypark.blogbackend.domain.chess.dto.GameStateMessage
import me.wypark.blogbackend.domain.chess.dto.MatchFoundMessage
import me.wypark.blogbackend.domain.chess.entity.ChessSide
import me.wypark.blogbackend.domain.chess.entity.OnlineGame
import me.wypark.blogbackend.domain.chess.entity.OnlineInvite
import me.wypark.blogbackend.domain.chess.entity.OnlinePlayer
import me.wypark.blogbackend.domain.chess.entity.TimeControl
import me.wypark.blogbackend.global.common.BusinessException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OnlineGameServiceTest {

    private lateinit var clock: MutableClock
    private lateinit var store: FakeOnlineGameStore
    private lateinit var history: FakeOnlineGameHistoryStore
    private lateinit var notifier: FakeNotifier
    private lateinit var service: OnlineGameService

    @BeforeEach
    fun setUp() {
        clock = MutableClock(Instant.parse("2026-09-05T00:00:00Z"))
        store = FakeOnlineGameStore()
        history = FakeOnlineGameHistoryStore()
        notifier = FakeNotifier()
        service = OnlineGameService(store, history, notifier, FakeStateEngine(), clock)
    }

    @Test
    fun `joining an invite starts a game and notifies both players`() {
        val invite = service.createInvite(ALICE, TimeControl.BLITZ_3)

        val game = service.joinInvite(BOB, invite.code.lowercase())

        assertEquals(setOf(ALICE.memberId, BOB.memberId), setOf(game.white.memberId, game.black.memberId))
        assertEquals(180_000L, game.whiteMillis)
        assertNull(game.lastMoveAt)
        assertEquals(setOf(game.gameId), store.activeGameIds())
        assertNull(store.findInvite(invite.code))
        assertEquals(game.gameId, notifier.lastOf<MatchFoundMessage>(ALICE.memberId)?.gameId)
        assertEquals(game.gameId, notifier.lastOf<MatchFoundMessage>(BOB.memberId)?.gameId)
    }

    @Test
    fun `player with an active game cannot queue or invite and can find it again`() {
        val game = startGame(TimeControl.RAPID_10)

        assertEquals(game.gameId, service.findActiveGame(ALICE.memberId)?.gameId)
        assertFailsWith<BusinessException> { service.joinQueue(ALICE, TimeControl.BLITZ_1) }
        assertFailsWith<BusinessException> { service.createInvite(BOB, TimeControl.BLITZ_1) }
        assertEquals(OnlineGame.IN_PROGRESS, history.find(game.gameId)?.status, "record is written at game start")

        service.resign(ALICE.memberId, game.gameId)

        assertNull(service.findActiveGame(ALICE.memberId))
        assertEquals(1, service.listGames(ALICE.memberId, Pageable.unpaged()).totalElements)
        assertEquals("LOSS", service.listGames(ALICE.memberId, Pageable.unpaged()).content.single().outcome)
    }

    @Test
    fun `host cannot join own invite`() {
        val invite = service.createInvite(ALICE, TimeControl.BLITZ_1)

        assertFailsWith<BusinessException> { service.joinInvite(ALICE, invite.code) }
    }

    @Test
    fun `queue pairs players waiting on the same time control`() {
        assertNull(service.joinQueue(ALICE, TimeControl.RAPID_10))
        assertNull(service.joinQueue(BOB, TimeControl.BLITZ_1))

        val game = service.joinQueue(CAROL, TimeControl.RAPID_10)

        assertNotNull(game)
        assertEquals(setOf(ALICE.memberId, CAROL.memberId), setOf(game.white.memberId, game.black.memberId))
        assertNull(service.joinQueue(DAVE, TimeControl.RAPID_10), "queue should be empty after pairing")
    }

    @Test
    fun `first move is untimed and increment applies once the clock runs`() {
        val game = startGame(TimeControl.RAPID_15_10)

        val afterWhite = service.move(game.white.memberId, game.gameId, "e2e4")
        assertEquals(900_000L, afterWhite.whiteMillis)
        assertNotNull(afterWhite.lastMoveAt)

        clock.advance(Duration.ofSeconds(5))
        val afterBlack = service.move(game.black.memberId, game.gameId, "e7e5")

        assertEquals(900_000L - 5_000L + 10_000L, afterBlack.blackMillis)
        assertEquals(ChessSide.WHITE, afterBlack.turn)
    }

    @Test
    fun `player whose clock runs out loses on tick`() {
        val game = startGame(TimeControl.BLITZ_1)
        service.move(game.white.memberId, game.gameId, "e2e4")

        clock.advance(Duration.ofSeconds(61))
        service.tick()

        val finished = assertNotNull(history.find(game.gameId))
        assertEquals(OnlineGame.TIMEOUT, finished.status)
        assertEquals("1-0", finished.result)
        assertTrue(store.activeGameIds().isEmpty())
        assertEquals(OnlineGame.TIMEOUT, notifier.lastOf<GameStateMessage>(game.black.memberId)?.game?.status)
    }

    @Test
    fun `game without a first move is aborted`() {
        val game = startGame(TimeControl.RAPID_10)

        clock.advance(Duration.ofSeconds(60))
        service.tick()

        assertEquals(OnlineGame.ABORTED, history.find(game.gameId)?.status)
        assertNull(history.find(game.gameId)?.result)
    }

    @Test
    fun `disconnected player on turn forfeits after the grace period unless they return`() {
        val game = startGame(TimeControl.RAPID_10)
        service.move(game.white.memberId, game.gameId, "e2e4")
        service.move(game.black.memberId, game.gameId, "e7e5")

        service.onDisconnected(game.white.memberId)
        val waiting = assertNotNull(store.find(game.gameId))
        assertEquals(clock.instant().plus(OnlineGameService.FORFEIT_GRACE), waiting.forfeitDeadlineAt)

        clock.advance(Duration.ofSeconds(30))
        service.subscribe(game.white.memberId, game.gameId)
        assertNull(store.find(game.gameId)?.forfeitDeadlineAt)
        assertTrue(store.find(game.gameId)?.disconnected.orEmpty().isEmpty())

        service.onDisconnected(game.white.memberId)
        clock.advance(Duration.ofSeconds(89))
        service.tick()
        assertEquals(OnlineGame.IN_PROGRESS, store.find(game.gameId)?.status)

        clock.advance(Duration.ofSeconds(2))
        service.tick()
        val finished = assertNotNull(history.find(game.gameId))
        assertEquals(OnlineGame.ABANDONED, finished.status)
        assertEquals("0-1", finished.result)
    }

    @Test
    fun `disconnect off turn only starts the countdown when the turn arrives`() {
        val game = startGame(TimeControl.RAPID_10)
        service.move(game.white.memberId, game.gameId, "e2e4")

        service.onDisconnected(game.white.memberId)
        assertNull(store.find(game.gameId)?.forfeitDeadlineAt)
        assertEquals(setOf(ChessSide.WHITE), store.find(game.gameId)?.disconnected)

        clock.advance(Duration.ofSeconds(10))
        val afterBlack = service.move(game.black.memberId, game.gameId, "e7e5")

        assertEquals(clock.instant().plus(OnlineGameService.FORFEIT_GRACE), afterBlack.forfeitDeadlineAt)
    }

    @Test
    fun `draw offer accepted by the opponent ends the game`() {
        val game = startGame(TimeControl.BLITZ_3)
        service.move(game.white.memberId, game.gameId, "e2e4")

        val offered = service.offerDraw(game.black.memberId, game.gameId)
        assertEquals(ChessSide.BLACK, offered.drawOfferedBy)

        val finished = service.acceptDraw(game.white.memberId, game.gameId)
        assertEquals(OnlineGame.DRAW_AGREED, finished.status)
        assertEquals("1/2-1/2", finished.result)
        assertTrue(finished.pgn.endsWith("1/2-1/2"))
    }

    @Test
    fun `illegal move and wrong turn are rejected`() {
        val game = startGame(TimeControl.BLITZ_3)

        assertFailsWith<BusinessException> { service.move(game.black.memberId, game.gameId, "e7e5") }
        val rejected = assertFailsWith<BusinessException> { service.move(game.white.memberId, game.gameId, "bad") }
        assertEquals("둘 수 없는 수입니다.", rejected.message)
        assertEquals(emptyList(), store.find(game.gameId)?.moves)
    }

    private fun startGame(timeControl: TimeControl): OnlineGame {
        val invite = service.createInvite(ALICE, timeControl)
        return service.joinInvite(BOB, invite.code)
    }

    companion object {
        private val ALICE = OnlinePlayer(1L, "alice")
        private val BOB = OnlinePlayer(2L, "bob")
        private val CAROL = OnlinePlayer(3L, "carol")
        private val DAVE = OnlinePlayer(4L, "dave")
    }
}

private class MutableClock(private var current: Instant) : Clock() {
    fun advance(duration: Duration) {
        current = current.plus(duration)
    }

    override fun instant(): Instant = current

    override fun getZone(): ZoneId = ZoneOffset.UTC

    override fun withZone(zone: ZoneId): Clock = this
}

private class FakeOnlineGameStore : OnlineGameStore {
    private val games = mutableMapOf<String, OnlineGame>()
    private val invites = mutableMapOf<String, OnlineInvite>()

    override fun save(game: OnlineGame) {
        games[game.gameId] = game
    }

    override fun find(gameId: String): OnlineGame? = games[gameId]

    override fun activeGameIds(): Set<String> = games.values.filter { it.isActive }.map { it.gameId }.toSet()

    override fun saveInvite(invite: OnlineInvite) {
        invites[invite.code] = invite
    }

    override fun findInvite(code: String): OnlineInvite? = invites[code]

    override fun deleteInvite(code: String) {
        invites.remove(code)
    }
}

private class FakeOnlineGameHistoryStore : OnlineGameHistoryStore {
    private val games = mutableMapOf<String, OnlineGame>()

    override fun save(game: OnlineGame) {
        games[game.gameId] = game
    }

    override fun find(gameId: String): OnlineGame? = games[gameId]

    override fun findAllByMemberId(memberId: Long, pageable: Pageable): Page<OnlineGame> {
        return PageImpl(games.values.filter { it.sideOf(memberId) != null })
    }
}

private class FakeNotifier : OnlineGameNotifier {
    private val messages = mutableMapOf<Long, MutableList<Any>>()

    override fun send(memberId: Long, message: Any) {
        messages.getOrPut(memberId) { mutableListOf() }.add(message)
    }

    override fun isConnected(memberId: Long): Boolean = true

    inline fun <reified T> lastOf(memberId: Long): T? = messages[memberId]?.filterIsInstance<T>()?.lastOrNull()
}

/** 합법성 검사 대신 "bad"만 거절하고, 수 개수로 차례를 계산하는 상태 엔진. */
private class FakeStateEngine : MaiaEngine {
    override fun getState(request: MaiaStateRequest): MaiaStateResponse {
        if ("bad" in request.moves) throw BusinessException("rejected", "MAIA_REJECTED")
        val turn = if (request.moves.size % 2 == 0) "white" else "black"
        return MaiaStateResponse(
            fen = "fen-${request.moves.size}",
            turn = turn,
            status = OnlineGame.IN_PROGRESS,
            result = null,
            pgn = "[Result \"*\"]\n\n${request.moves.joinToString(" ")} *"
        )
    }

    override fun playMove(request: MaiaPlayRequest): MaiaPlayResponse {
        throw UnsupportedOperationException("online games never ask Maia to play")
    }
}
