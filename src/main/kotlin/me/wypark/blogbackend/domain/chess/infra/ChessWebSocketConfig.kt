package me.wypark.blogbackend.domain.chess.infra

import me.wypark.blogbackend.global.config.CorsProperties
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

@Configuration
@EnableWebSocket
class ChessWebSocketConfig(
    private val handler: ChessWebSocketHandler,
    private val corsProperties: CorsProperties
) : WebSocketConfigurer {

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(handler, "/ws/chess")
            .setAllowedOrigins(*corsProperties.allowedOrigins.toTypedArray())
    }
}
