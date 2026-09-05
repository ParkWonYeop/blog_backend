package me.wypark.blogbackend.domain.chess.infra

import com.fasterxml.jackson.databind.ObjectMapper
import me.wypark.blogbackend.domain.chess.dto.AuthOkMessage
import me.wypark.blogbackend.domain.chess.dto.ErrorMessage
import me.wypark.blogbackend.domain.chess.dto.InviteCreatedMessage
import me.wypark.blogbackend.domain.chess.dto.OnlineClientMessage
import me.wypark.blogbackend.domain.chess.dto.QueueJoinedMessage
import me.wypark.blogbackend.domain.chess.dto.SimpleMessage
import me.wypark.blogbackend.domain.chess.dto.TimeControlResponse
import me.wypark.blogbackend.domain.chess.entity.OnlinePlayer
import me.wypark.blogbackend.domain.chess.entity.TimeControl
import me.wypark.blogbackend.domain.chess.service.OnlineGameService
import me.wypark.blogbackend.global.common.BusinessException
import me.wypark.blogbackend.global.security.AuthenticatedUser
import me.wypark.blogbackend.global.security.JwtProvider
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Component
class ChessWebSocketHandler(
    private val registry: ChessSocketRegistry,
    private val onlineGameService: OnlineGameService,
    private val jwtProvider: JwtProvider,
    private val objectMapper: ObjectMapper,
    private val clock: Clock
) : TextWebSocketHandler() {

    private val log = LoggerFactory.getLogger(javaClass)
    private val decorated = ConcurrentHashMap<String, WebSocketSession>()
    private val pendingSince = ConcurrentHashMap<String, Instant>()
    private val players = ConcurrentHashMap<String, OnlinePlayer>()

    override fun afterConnectionEstablished(session: WebSocketSession) {
        decorated[session.id] = registry.decorate(session)
        pendingSince[session.id] = Instant.now(clock)
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val socket = decorated[session.id] ?: return
        val request = try {
            objectMapper.readValue(message.payload, OnlineClientMessage::class.java)
        } catch (e: Exception) {
            registry.sendTo(socket, ErrorMessage("메시지 형식이 올바르지 않습니다.", "BAD_MESSAGE"))
            return
        }

        val player = players[session.id]
        if (player == null) {
            if (request.type == "AUTH") authenticate(session, socket, request.token) else {
                registry.sendTo(socket, ErrorMessage("먼저 인증해야 합니다.", "AUTH_REQUIRED"))
            }
            return
        }

        try {
            dispatch(player, request, socket)
        } catch (e: BusinessException) {
            registry.sendTo(socket, ErrorMessage(e.message, e.code))
        } catch (e: IllegalArgumentException) {
            registry.sendTo(socket, ErrorMessage(e.message ?: "잘못된 요청입니다.", "BAD_REQUEST"))
        } catch (e: Exception) {
            log.error("websocket request failed: type={}", request.type, e)
            registry.sendTo(socket, ErrorMessage("요청을 처리하지 못했습니다.", "INTERNAL"))
        }
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        decorated.remove(session.id)
        pendingSince.remove(session.id)
        players.remove(session.id)
        val memberId = registry.unregister(session) ?: return
        try {
            onlineGameService.onDisconnected(memberId)
        } catch (e: Exception) {
            log.error("failed to handle disconnect for member {}", memberId, e)
        }
    }

    override fun handleTransportError(session: WebSocketSession, exception: Throwable) {
        log.debug("websocket transport error on {}: {}", session.id, exception.message)
    }

    /** 인증 없이 열어 둔 연결은 곧 닫아 익명 연결이 쌓이지 않게 한다. */
    @Scheduled(fixedDelay = 5_000)
    fun closeUnauthenticated() {
        val cutoff = Instant.now(clock).minus(AUTH_TIMEOUT)
        pendingSince.entries
            .filter { it.value.isBefore(cutoff) }
            .forEach { (sessionId, _) ->
                pendingSince.remove(sessionId)
                decorated[sessionId]?.let { closeQuietly(it, CloseStatus.POLICY_VIOLATION) }
            }
    }

    private fun authenticate(session: WebSocketSession, socket: WebSocketSession, token: String?) {
        if (token.isNullOrBlank() || !jwtProvider.isValid(token)) {
            registry.sendTo(socket, ErrorMessage("인증에 실패했습니다.", "UNAUTHORIZED"))
            closeQuietly(socket, CloseStatus.POLICY_VIOLATION)
            return
        }
        val principal = jwtProvider.getAuthentication(token).principal as AuthenticatedUser
        val player = OnlinePlayer(principal.memberId, principal.nickname)
        players[session.id] = player
        pendingSince.remove(session.id)
        registry.register(player.memberId, socket)
        registry.sendTo(socket, AuthOkMessage(player.memberId, player.nickname))
    }

    private fun dispatch(player: OnlinePlayer, request: OnlineClientMessage, socket: WebSocketSession) {
        when (request.type) {
            "INVITE_CREATE" -> {
                val invite = onlineGameService.createInvite(player, TimeControl.from(request.timeControl))
                registry.sendTo(socket, InviteCreatedMessage(invite.code, TimeControlResponse.from(invite.timeControl)))
            }
            "INVITE_CANCEL" -> {
                onlineGameService.cancelInvite(player.memberId)
                registry.sendTo(socket, SimpleMessage("INVITE_CANCELLED"))
            }
            "INVITE_JOIN" -> onlineGameService.joinInvite(player, requireField(request.code, "code"))
            "QUEUE_JOIN" -> {
                val timeControl = TimeControl.from(request.timeControl)
                if (onlineGameService.joinQueue(player, timeControl) == null) {
                    registry.sendTo(socket, QueueJoinedMessage(TimeControlResponse.from(timeControl)))
                }
            }
            "QUEUE_LEAVE" -> {
                onlineGameService.leaveQueue(player.memberId)
                registry.sendTo(socket, SimpleMessage("QUEUE_LEFT"))
            }
            "SUBSCRIBE" -> onlineGameService.subscribe(player.memberId, requireField(request.gameId, "gameId"))
            "MOVE" -> onlineGameService.move(
                player.memberId,
                requireField(request.gameId, "gameId"),
                requireField(request.move, "move")
            )
            "RESIGN" -> onlineGameService.resign(player.memberId, requireField(request.gameId, "gameId"))
            "DRAW_OFFER" -> onlineGameService.offerDraw(player.memberId, requireField(request.gameId, "gameId"))
            "DRAW_ACCEPT" -> onlineGameService.acceptDraw(player.memberId, requireField(request.gameId, "gameId"))
            "DRAW_DECLINE" -> onlineGameService.declineDraw(player.memberId, requireField(request.gameId, "gameId"))
            "PING" -> registry.sendTo(socket, SimpleMessage("PONG"))
            else -> registry.sendTo(socket, ErrorMessage("알 수 없는 요청입니다.", "UNKNOWN_TYPE"))
        }
    }

    private fun requireField(value: String?, name: String): String {
        if (value.isNullOrBlank()) throw IllegalArgumentException("$name 값이 필요합니다.")
        return value
    }

    private fun closeQuietly(session: WebSocketSession, status: CloseStatus) {
        try {
            session.close(status)
        } catch (e: Exception) {
            log.debug("failed to close websocket session {}: {}", session.id, e.message)
        }
    }

    companion object {
        private val AUTH_TIMEOUT: Duration = Duration.ofSeconds(10)
    }
}
