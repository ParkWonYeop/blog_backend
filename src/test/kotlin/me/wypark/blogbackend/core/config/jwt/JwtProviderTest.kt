package me.wypark.blogbackend.core.config.jwt

import me.wypark.blogbackend.application.auth.AuthenticatedUser
import me.wypark.blogbackend.core.config.JwtProperties
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertIs

class JwtProviderTest {

    private val jwtProvider = JwtProvider(
        properties = JwtProperties(
            secret = "c2VjcmV0LWtleS1mb3ItdGVzdC1hZG1pbi1kYXNoYm9hcmQtMzItYnl0ZXM=",
            accessTokenValidity = 600000,
            refreshTokenValidity = 604800000
        ),
        clock = Clock.fixed(Instant.parse("2026-06-19T00:00:00Z"), ZoneOffset.UTC)
    )

