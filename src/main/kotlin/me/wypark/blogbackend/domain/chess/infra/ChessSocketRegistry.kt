package me.wypark.blogbackend.domain.chess.infra

import com.fasterxml.jackson.databind.ObjectMapper
import me.wypark.blogbackend.domain.chess.service.OnlineGameNotifier
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.PingMessage
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator
import java.util.concurrent.ConcurrentHashMap

/** 회원당 WebSocket 세션 하나를 유지한다. 새 연결이 오면 이전 연결을 닫는다. */
@Component
class ChessSocketRegistry(
    private val objectMapper: ObjectMapper
) : OnlineGameNotifier {

    private val log = LoggerFactory.getLogger(javaClass)
    private val sessions = ConcurrentHashMap<Long, WebSocketSession>()

    fun decorate(session: WebSocketSession): WebSocketSession {
        return ConcurrentWebSocketSessionDecorator(session, SEND_TIMEOUT_MILLIS, SEND_BUFFER_BYTES)
    }

    /** 세션을 등록하고, 같은 회원의 이전 세션이 있으면 닫는다. */
    fun register(memberId: Long, session: WebSocketSession) {
        val previous = sessions.put(memberId, session)
        if (previous != null && previous.id != session.id) {
            close(previous, REPLACED)
        }
    }

    /** 이 세션이 현재 등록된 세션일 때만 제거하고 회원 ID를 돌려준다. 교체된 옛 세션의 종료는 무시된다. */
    fun unregister(session: WebSocketSession): Long? {
        val entry = sessions.entries.firstOrNull { it.value.id == session.id } ?: return null
        return if (sessions.remove(entry.key, entry.value)) entry.key else null
    }

    override fun send(memberId: Long, message: Any) {
        sessions[memberId]?.let { sendTo(it, message) }
    }

    override fun isConnected(memberId: Long): Boolean = sessions[memberId]?.isOpen == true

    fun sendTo(session: WebSocketSession, message: Any) {
        if (!session.isOpen) return
        try {
            session.sendMessage(TextMessage(objectMapper.writeValueAsString(message)))
        } catch (e: Exception) {
            log.warn("failed to send websocket message to {}", session.id, e)
            close(session, CloseStatus.SESSION_NOT_RELIABLE)
        }
    }

    // Cloudflare는 100초 동안 조용한 WebSocket을 끊으므로 그 전에 ping을 보낸다.
    @Scheduled(fixedDelay = 30_000)
    fun ping() {
        sessions.values.forEach { session ->
            try {
                session.sendMessage(PingMessage())
            } catch (e: Exception) {
                log.debug("ping failed for {}: {}", session.id, e.message)
                close(session, CloseStatus.SESSION_NOT_RELIABLE)
            }
        }
    }

    private fun close(session: WebSocketSession, status: CloseStatus) {
        try {
            session.close(status)
        } catch (e: Exception) {
            log.debug("failed to close websocket session {}: {}", session.id, e.message)
        }
    }

    companion object {
        val REPLACED: CloseStatus = CloseStatus(4001, "replaced by a newer connection")
        private const val SEND_TIMEOUT_MILLIS = 5_000
        private const val SEND_BUFFER_BYTES = 256 * 1024
    }
}
