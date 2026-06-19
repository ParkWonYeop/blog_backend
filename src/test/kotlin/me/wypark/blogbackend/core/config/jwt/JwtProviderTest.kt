package me.wypark.blogbackend.core.config.jwt

import me.wypark.blogbackend.domain.auth.CustomUserDetails
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import kotlin.test.assertEquals
import kotlin.test.assertIs

class JwtProviderTest {

    private val jwtProvider = JwtProvider(
        secretKey = "c2VjcmV0LWtleS1mb3ItdGVzdC1hZG1pbi1kYXNoYm9hcmQtMzItYnl0ZXM=",
        accessTokenValidity = 600000,
        refreshTokenValidity = 604800000
    )

    @Test
    fun `access token authentication restores custom user details`() {
        val originalPrincipal = CustomUserDetails(
            memberId = 2L,
            nickname = "tester",
            username = "tester@example.com",
            password = "",
            authorities = listOf(SimpleGrantedAuthority("ROLE_ADMIN"))
        )
        val token = jwtProvider.generateTokenDto(
            UsernamePasswordAuthenticationToken(
                originalPrincipal,
                "",
                originalPrincipal.authorities
            )
        )

        val authentication = jwtProvider.getAuthentication(token.accessToken)
        val principal = assertIs<CustomUserDetails>(authentication.principal)

        assertEquals(2L, principal.memberId)
        assertEquals("tester", principal.nickname)
        assertEquals("tester@example.com", principal.username)
        assertEquals("ROLE_ADMIN", authentication.authorities.single().authority)
    }
}
