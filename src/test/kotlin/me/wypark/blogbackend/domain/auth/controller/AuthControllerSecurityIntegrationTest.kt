package me.wypark.blogbackend.domain.auth.controller

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerSecurityIntegrationTest(
    @Autowired private val mockMvc: MockMvc
) {

    @Test
    fun `logout requires an authenticated access token`() {
        mockMvc.perform(post("/api/auth/logout"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
    }

    @Test
    fun `reissue does not accept a refresh token from the request body`() {
        mockMvc.perform(
            post("/api/auth/reissue")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refreshToken":"attacker-readable-token"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.data").doesNotExist())
    }
}
