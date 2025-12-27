package me.wypark.blogbackend.core.config.jwt

import io.jsonwebtoken.*
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import me.wypark.blogbackend.api.dto.TokenDto
import me.wypark.blogbackend.domain.auth.CustomUserDetails // 👈 Import 추가
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtProvider(
    @Value("\${jwt.secret}") secretKey: String,
    @Value("\${jwt.access-token-validity}") private val accessTokenValidity: Long,
    @Value("\${jwt.refresh-token-validity}") private val refreshTokenValidity: Long
) {
    private val key: SecretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey))

    // 1. 토큰 생성 (Access + Refresh 동시 발급)
    fun generateTokenDto(authentication: Authentication): TokenDto {
        val authorities = authentication.authorities.joinToString(",") { it.authority }
        val now = Date().time

        // 👇 [수정] Principal을 CustomUserDetails로 캐스팅하여 정보 추출
        val principal = authentication.principal as CustomUserDetails
        val memberId = principal.memberId
        val nickname = principal.nickname

        // Access Token 생성
        val accessTokenExpiresIn = Date(now + accessTokenValidity)
        val accessToken = Jwts.builder()
            .subject(authentication.name) // email
            .claim("auth", authorities)   // 권한 정보 (ROLE_USER 등)
            .claim("memberId", memberId)  // 👈 [추가] 프론트엔드 식별용 ID
            .claim("nickname", nickname)  // 👈 [추가] 프론트엔드 표기용 닉네임
            .expiration(accessTokenExpiresIn)
            .signWith(key)
            .compact()

        // Refresh Token 생성 (권한 정보 등은 제외하고 만료일만 설정)
        val refreshToken = Jwts.builder()
            .subject(authentication.name)
            .expiration(Date(now + refreshTokenValidity))
            .signWith(key)
            .compact()

        return TokenDto(
            accessToken = accessToken,
            refreshToken = refreshToken,
            accessTokenExpiresIn = accessTokenExpiresIn.time
        )
    }

    // 2. 토큰에서 인증 정보(Authentication) 추출
    fun getAuthentication(accessToken: String): Authentication {
        val claims = parseClaims(accessToken)

        if (claims["auth"] == null) {
            throw RuntimeException("권한 정보가 없는 토큰입니다.")
        }

        val authorities: Collection<GrantedAuthority> =
            claims["auth"].toString()
                .split(",")
                .map { SimpleGrantedAuthority(it) }

        val principal = User(claims.subject, "", authorities)
        return UsernamePasswordAuthenticationToken(principal, "", authorities)
    }

    // 3. 토큰 검증 (만료 여부, 위변조 여부 확인)
    fun validateToken(token: String): Boolean {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token)
            return true
        } catch (e: SecurityException) {
            // log.info("잘못된 JWT 서명입니다.")
        } catch (e: MalformedJwtException) {
            // log.info("잘못된 JWT 서명입니다.")
        } catch (e: ExpiredJwtException) {
            // log.info("만료된 JWT 토큰입니다.")
        } catch (e: UnsupportedJwtException) {
            // log.info("지원되지 않는 JWT 토큰입니다.")
        } catch (e: IllegalArgumentException) {
            // log.info("JWT 토큰이 잘못되었습니다.")
        }
        return false
    }

    private fun parseClaims(accessToken: String): Claims {
        return try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(accessToken).payload
        } catch (e: ExpiredJwtException) {
            e.claims
        }
    }
}