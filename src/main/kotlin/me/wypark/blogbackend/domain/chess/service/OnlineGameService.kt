package me.wypark.blogbackend.domain.chess.service

import me.wypark.blogbackend.domain.chess.dto.GameStateMessage
import me.wypark.blogbackend.domain.chess.dto.MatchFoundMessage
import me.wypark.blogbackend.domain.chess.dto.OnlineGameResponse
import me.wypark.blogbackend.domain.chess.dto.OnlineGameSummaryResponse
import me.wypark.blogbackend.domain.chess.entity.ChessSide
import me.wypark.blogbackend.domain.chess.entity.OnlineGame
import me.wypark.blogbackend.domain.chess.entity.OnlineInvite
import me.wypark.blogbackend.domain.chess.entity.OnlinePlayer
import me.wypark.blogbackend.domain.chess.entity.TimeControl
import me.wypark.blogbackend.domain.chess.entity.withPgnResult
import me.wypark.blogbackend.global.common.BusinessException
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.EnumMap
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.random.Random

@Service
class OnlineGameService(
    private val store: OnlineGameStore,
    private val historyStore: OnlineGameHistoryStore,
    private val notifier: OnlineGameNotifier,
    private val maiaEngine: MaiaEngine,
    private val clock: Clock
) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val gameLocks = ConcurrentHashMap<String, ReentrantLock>()
    private val queueLock = Any()
    private val queues = EnumMap<TimeControl, LinkedHashMap<Long, OnlinePlayer>>(TimeControl::class.java)
    private val hostedInvites = ConcurrentHashMap<Long, String>()
    private val codeRandom = SecureRandom()

    fun createInvite(host: OnlinePlayer, timeControl: TimeControl): OnlineInvite {
        requireNoActiveGame(host.memberId)
        hostedInvites.remove(host.memberId)?.let(store::deleteInvite)
        val invite = OnlineInvite(generateCode(), host, timeControl, now())
        store.saveInvite(invite)
        hostedInvites[host.memberId] = invite.code
        return invite
    }

    fun cancelInvite(memberId: Long) {
        hostedInvites.remove(memberId)?.let(store::deleteInvite)
    }

    fun joinInvite(guest: OnlinePlayer, code: String): OnlineGame {
        val invite = store.findInvite(code.trim().uppercase())
            ?: throw BusinessException.notFound("초대를 찾을 수 없습니다.")
        if (invite.host.memberId == guest.memberId) throw BusinessException("자신의 초대에는 참가할 수 없습니다.")
        requireNoActiveGame(guest.memberId)
        store.deleteInvite(invite.code)
        hostedInvites.remove(invite.host.memberId)
        return startGame(invite.host, guest, invite.timeControl)
    }

    /** 같은 시간 설정으로 기다리는 사람이 있으면 바로 대국을 만들고, 없으면 대기열에 넣고 null을 돌려준다. */
    fun joinQueue(player: OnlinePlayer, timeControl: TimeControl): OnlineGame? {
        requireNoActiveGame(player.memberId)
        val opponent = synchronized(queueLock) {
            queues.values.forEach { it.remove(player.memberId) }
            val queue = queues.getOrPut(timeControl) { LinkedHashMap() }
            val waiting = queue.entries.firstOrNull()
            if (waiting == null) {
                queue[player.memberId] = player
                null
            } else {
                queue.remove(waiting.key)
                waiting.value
            }
        }
        return opponent?.let { startGame(it, player, timeControl) }
    }

    fun leaveQueue(memberId: Long) {
        synchronized(queueLock) {
            queues.values.forEach { it.remove(memberId) }
        }
    }

    /** 진행 중인 내 대국. 로비에 들어올 때 이어서 두도록 안내하는 데 쓴다. */
    fun findActiveGame(memberId: Long): OnlineGameResponse? {
        return activeGameIdsOf(memberId).firstNotNullOfOrNull(store::find)?.let { OnlineGameResponse.from(it, now()) }
    }

    fun listGames(memberId: Long, pageable: Pageable): Page<OnlineGameSummaryResponse> {
        return historyStore.findAllByMemberId(memberId, pageable).map { OnlineGameSummaryResponse.from(it, memberId) }
    }

    fun getGame(memberId: Long, gameId: String): OnlineGameResponse {
        val game = store.find(gameId) ?: historyStore.find(gameId)
            ?: throw BusinessException.notFound("대국을 찾을 수 없습니다.")
        if (game.sideOf(memberId) == null) throw BusinessException.notFound("대국을 찾을 수 없습니다.")
        return OnlineGameResponse.from(game, now())
    }

    /** 대국 화면 진입·재접속. 끊겼던 쪽이면 복귀로 처리하고 양쪽에 상태를 다시 보낸다. */
    fun subscribe(memberId: Long, gameId: String) {
        val stored = store.find(gameId)
        if (stored == null) {
            val finished = historyStore.find(gameId) ?: throw BusinessException.notFound("대국을 찾을 수 없습니다.")
            participant(finished, memberId)
            notifier.send(memberId, GameStateMessage(OnlineGameResponse.from(finished, now())))
            return
        }
        withGame(gameId) { game ->
            val side = participant(game, memberId)
            if (!game.isActive || side !in game.disconnected) {
                notifier.send(memberId, GameStateMessage(OnlineGameResponse.from(game, now())))
                return@withGame game
            }
            val reconnected = game.copy(
                disconnected = game.disconnected - side,
                forfeitDeadlineAt = if (game.turn == side) null else game.forfeitDeadlineAt
            )
            store.save(reconnected)
            broadcast(reconnected)
            reconnected
        }
    }

    fun move(memberId: Long, gameId: String, uci: String): OnlineGame = withGame(gameId) { game ->
        val side = participant(game, memberId)
        requireActive(game)
        if (game.turn != side) throw BusinessException("상대 차례입니다.")

        val now = now()
        val remaining = game.remainingMillis(side, now)
        if (remaining <= 0) return@withGame finish(game, OnlineGame.TIMEOUT, side.opposite().winResult(), now)

        val moves = game.moves + uci
        val state = try {
            maiaEngine.getState(stateRequest(game, moves))
        } catch (e: BusinessException) {
            if (e.code == "MAIA_REJECTED") throw BusinessException("둘 수 없는 수입니다.")
            throw e
        }

        val increment = if (game.lastMoveAt != null) game.timeControl.incrementMillis else 0L
        val opponent = side.opposite()
        val moved = game
            .withMillis(side, remaining + increment)
            .copy(
                moves = moves,
                fen = state.fen,
                turn = ChessSide.from(state.turn),
                status = state.status,
                result = state.result,
                pgn = state.pgn,
                lastMoveAt = now,
                drawOfferedBy = null,
                forfeitDeadlineAt = if (opponent in game.disconnected) now.plus(FORFEIT_GRACE) else null
            )
        if (!moved.isActive) return@withGame finish(moved, moved.status, moved.result, now)

        store.save(moved)
        broadcast(moved)
        moved
    }

    fun resign(memberId: Long, gameId: String): OnlineGame = withGame(gameId) { game ->
        val side = participant(game, memberId)
        requireActive(game)
        finish(game, OnlineGame.RESIGNED, side.opposite().winResult(), now())
    }

    fun offerDraw(memberId: Long, gameId: String): OnlineGame = withGame(gameId) { game ->
        val side = participant(game, memberId)
        requireActive(game)
        if (game.drawOfferedBy == side.opposite()) {
            return@withGame finish(game, OnlineGame.DRAW_AGREED, OnlineGame.DRAW_RESULT, now())
        }
        val offered = game.copy(drawOfferedBy = side)
        store.save(offered)
        broadcast(offered)
        offered
    }

    fun acceptDraw(memberId: Long, gameId: String): OnlineGame = withGame(gameId) { game ->
        val side = participant(game, memberId)
        requireActive(game)
        if (game.drawOfferedBy != side.opposite()) throw BusinessException("무승부 제안이 없습니다.")
        finish(game, OnlineGame.DRAW_AGREED, OnlineGame.DRAW_RESULT, now())
    }

    fun declineDraw(memberId: Long, gameId: String): OnlineGame = withGame(gameId) { game ->
        val side = participant(game, memberId)
        if (!game.isActive || game.drawOfferedBy != side.opposite()) return@withGame game
        val declined = game.copy(drawOfferedBy = null)
        store.save(declined)
        broadcast(declined)
        declined
    }

    /** WebSocket이 끊기면 대기열·초대에서 빼고, 진행 중 대국에서는 차례일 때만 기권 카운트다운을 건다. */
    fun onDisconnected(memberId: Long) {
        leaveQueue(memberId)
        cancelInvite(memberId)
        activeGameIdsOf(memberId).forEach { gameId ->
            withGame<OnlineGame>(gameId) { game ->
                val side = game.sideOf(memberId)
                if (side == null || !game.isActive || side in game.disconnected) return@withGame game
                val disconnected = game.copy(
                    disconnected = game.disconnected + side,
                    forfeitDeadlineAt = if (game.turn == side) now().plus(FORFEIT_GRACE) else game.forfeitDeadlineAt
                )
                store.save(disconnected)
                broadcast(disconnected)
                disconnected
            }
        }
    }

    @Scheduled(fixedDelay = 1000)
    fun tick() {
        val activeIds = try {
            store.activeGameIds()
        } catch (e: Exception) {
            log.debug("online game tick skipped: {}", e.message)
            return
        }
        activeIds.forEach { gameId ->
            try {
                withGame<OnlineGame>(gameId) { game -> settle(game, now()) }
            } catch (e: Exception) {
                log.warn("online game tick failed for {}", gameId, e)
            }
        }
    }

    private fun settle(game: OnlineGame, now: Instant): OnlineGame {
        if (!game.isActive) {
            store.save(game)
            return game
        }
        if (game.lastMoveAt == null) {
            val waited = Duration.between(game.createdAt, now)
            return if (waited >= FIRST_MOVE_TIMEOUT) finish(game, OnlineGame.ABORTED, null, now) else game
        }
        if (game.remainingMillis(game.turn, now) <= 0) {
            return finish(game, OnlineGame.TIMEOUT, game.turn.opposite().winResult(), now)
        }
        val deadline = game.forfeitDeadlineAt
        if (deadline != null && !now.isBefore(deadline)) {
            return finish(game, OnlineGame.ABANDONED, game.turn.opposite().winResult(), now)
        }
        return game
    }

    private fun startGame(first: OnlinePlayer, second: OnlinePlayer, timeControl: TimeControl): OnlineGame {
        val (white, black) = if (Random.nextBoolean()) first to second else second to first
        val now = now()
        val disconnected = listOf(ChessSide.WHITE to white, ChessSide.BLACK to black)
            .filter { (_, player) -> !notifier.isConnected(player.memberId) }
            .map { (side, _) -> side }
            .toSet()
        val skeleton = OnlineGame(
            gameId = UUID.randomUUID().toString(),
            timeControl = timeControl,
            white = white,
            black = black,
            moves = emptyList(),
            fen = "",
            turn = ChessSide.WHITE,
            status = OnlineGame.IN_PROGRESS,
            result = null,
            pgn = "",
            whiteMillis = timeControl.initialMillis,
            blackMillis = timeControl.initialMillis,
            lastMoveAt = null,
            drawOfferedBy = null,
            disconnected = disconnected,
            forfeitDeadlineAt = if (ChessSide.WHITE in disconnected) now.plus(FORFEIT_GRACE) else null,
            createdAt = now,
            finishedAt = null
        )
        val state = maiaEngine.getState(stateRequest(skeleton, emptyList()))
        val game = skeleton.copy(fen = state.fen, turn = ChessSide.from(state.turn), pgn = state.pgn)
        store.save(game)
        historyStore.save(game)
        notifier.send(white.memberId, MatchFoundMessage(game.gameId))
        notifier.send(black.memberId, MatchFoundMessage(game.gameId))
        return game
    }

    private fun finish(game: OnlineGame, status: String, result: String?, now: Instant): OnlineGame {
        val finished = game.copy(
            status = status,
            result = result,
            pgn = game.pgn.withPgnResult(result ?: "*"),
            drawOfferedBy = null,
            forfeitDeadlineAt = null,
            finishedAt = now
        )
        store.save(finished)
        historyStore.save(finished)
        broadcast(finished)
        gameLocks.remove(game.gameId)
        return finished
    }

    private fun broadcast(game: OnlineGame) {
        val message = GameStateMessage(OnlineGameResponse.from(game, now()))
        notifier.send(game.white.memberId, message)
        notifier.send(game.black.memberId, message)
    }

    private fun <T> withGame(gameId: String, block: (OnlineGame) -> T): T {
        val lock = gameLocks.computeIfAbsent(gameId) { ReentrantLock() }
        return lock.withLock {
            val game = store.find(gameId) ?: throw BusinessException.notFound("대국을 찾을 수 없습니다.")
            block(game)
        }
    }

    private fun activeGameIdsOf(memberId: Long): List<String> {
        return store.activeGameIds().filter { store.find(it)?.sideOf(memberId) != null }
    }

    private fun participant(game: OnlineGame, memberId: Long): ChessSide {
        return game.sideOf(memberId) ?: throw BusinessException.notFound("대국을 찾을 수 없습니다.")
    }

    private fun requireActive(game: OnlineGame) {
        if (!game.isActive) throw BusinessException("이미 끝난 대국입니다.")
    }

    private fun requireNoActiveGame(memberId: Long) {
        if (activeGameIdsOf(memberId).isNotEmpty()) {
            throw BusinessException("진행 중인 대국이 있습니다. 먼저 그 대국을 끝내세요.", "ACTIVE_GAME_EXISTS")
        }
    }

    private fun stateRequest(game: OnlineGame, moves: List<String>): MaiaStateRequest {
        return MaiaStateRequest(moves = moves, white = game.white.nickname, black = game.black.nickname, event = "Online")
    }

    private fun generateCode(): String {
        return buildString { repeat(CODE_LENGTH) { append(CODE_ALPHABET[codeRandom.nextInt(CODE_ALPHABET.length)]) } }
    }

    private fun now(): Instant = Instant.now(clock)

    companion object {
        /** 차례인 쪽이 끊긴 뒤 이 시간 안에 돌아오지 않으면 기권 처리한다. */
        val FORFEIT_GRACE: Duration = Duration.ofSeconds(90)

        /** 백이 첫 수를 두지 않으면 대국을 무효 처리한다. */
        val FIRST_MOVE_TIMEOUT: Duration = Duration.ofSeconds(60)

        private const val CODE_LENGTH = 6

        // 헷갈리는 0/O, 1/I는 뺐다.
        private const val CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    }
}
