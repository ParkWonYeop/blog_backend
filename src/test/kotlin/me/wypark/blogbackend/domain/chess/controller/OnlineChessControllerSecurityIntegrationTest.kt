package me.wypark.blogbackend.domain.chess.controller

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OnlineChessControllerSecurityIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `online game detail requires login`() {
        mockMvc.perform(get("/api/chess/online/games/some-game"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `online game list and active lookup require login`() {
        mockMvc.perform(get("/api/chess/online/games")).andExpect(status().isUnauthorized)
        mockMvc.perform(get("/api/chess/online/games/active")).andExpect(status().isUnauthorized)
    }

    @Test
    fun `time controls require login`() {
        mockMvc.perform(get("/api/chess/online/time-controls"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `websocket handshake endpoint is not blocked by security`() {
        // 핸드셰이크 헤더가 없으면 400이지만, 인증 필터에 막혀 401이 나면 안 된다.
        mockMvc.perform(get("/ws/chess"))
            .andExpect(status().is4xxClientError)
            .andExpect(status().`is`(org.hamcrest.Matchers.not(401)))
    }

    @Test
    fun `unknown chess routes still return json errors`() {
        mockMvc.perform(get("/api/chess/online/games/x").header("Authorization", "Bearer invalid"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
    }
}
