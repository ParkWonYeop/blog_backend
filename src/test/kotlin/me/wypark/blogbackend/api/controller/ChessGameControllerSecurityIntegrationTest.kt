package me.wypark.blogbackend.api.controller

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChessGameControllerSecurityIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `chess game creation requires login`() {
        mockMvc.perform(
            post("/api/chess/games")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"rating":1500,"playerColor":"white","model":"5m"}""")
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `chess game history requires login`() {
        mockMvc.perform(get("/api/chess/games"))
            .andExpect(status().isUnauthorized)
    }
}
