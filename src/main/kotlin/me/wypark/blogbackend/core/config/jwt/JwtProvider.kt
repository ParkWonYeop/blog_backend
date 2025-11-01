package me.wypark.blogbackend.core.config.jwt

import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import me.wypark.blogbackend.application.auth.AuthenticatedUser
import me.wypark.blogbackend.application.auth.TokenDto
import me.wypark.blogbackend.application.auth.TokenProvider
import me.wypark.blogbackend.core.config.JwtProperties
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component
import java.time.Clock
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtProvider(
    private val properties: JwtProperties,
    private val clock: Clock
) : TokenProvider {
    private val signingKey: SecretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret))

    override fun generate(authentication: Authentication): TokenDto {
        val principal = authentication.principal as AuthenticatedUser
        val issuedAt = clock.millis()
        val accessTokenExpiresAt = Date(issuedAt + properties.accessTokenValidity)

