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

        val accessToken = Jwts.builder()
            .subject(authentication.name)
            .claim(AUTHORITIES_CLAIM, authentication.authorities.joinToString(",") { it.authority })
            .claim(MEMBER_ID_CLAIM, principal.memberId)
            .claim(NICKNAME_CLAIM, principal.nickname)
            .expiration(accessTokenExpiresAt)
            .signWith(signingKey)
            .compact()

        val refreshToken = Jwts.builder()
            .subject(authentication.name)
            .expiration(Date(issuedAt + properties.refreshTokenValidity))
            .signWith(signingKey)
            .compact()

        return TokenDto(
            accessToken = accessToken,
            refreshToken = refreshToken,
            accessTokenExpiresIn = accessTokenExpiresAt.time
        )
    }

    fun getAuthentication(accessToken: String): Authentication {
        val claims = parseClaims(accessToken)
        val authorityClaim = claims[AUTHORITIES_CLAIM]
            ?: throw IllegalArgumentException("권한 정보가 없는 토큰입니다.")
        val authorities: Collection<GrantedAuthority> = authorityClaim.toString()
            .split(',')
            .map(::SimpleGrantedAuthority)
        val memberId = when (val claim = claims[MEMBER_ID_CLAIM]) {
            is Number -> claim.toLong()
            is String -> claim.toLong()
            else -> throw IllegalArgumentException("memberId claim is missing.")
        }
        val principal = AuthenticatedUser(
            memberId = memberId,
            nickname = claims[NICKNAME_CLAIM]?.toString() ?: claims.subject,
            username = claims.subject,
            password = "",
            authorities = authorities
        )
        return UsernamePasswordAuthenticationToken(principal, "", authorities)
    }

    override fun isValid(token: String): Boolean {
        return try {
            Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token)
            true
        } catch (_: JwtException) {
            false
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    override fun extractSubject(token: String): String = parseClaims(token).subject

    private fun parseClaims(accessToken: String): Claims {
        return try {
            Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(accessToken).payload
        } catch (exception: ExpiredJwtException) {
            exception.claims
        }
    }
    companion object {
        private const val AUTHORITIES_CLAIM = "auth"
        private const val MEMBER_ID_CLAIM = "memberId"
        private const val NICKNAME_CLAIM = "nickname"
    }
}
