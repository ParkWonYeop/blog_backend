package me.wypark.blogbackend.core.config.jwt

import io.jsonwebtoken.*
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import me.wypark.blogbackend.api.dto.TokenDto
import me.wypark.blogbackend.domain.auth.CustomUserDetails
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.SecretKey

/**
 * [JWT 토큰 관리자]
 *
 * JWT(Json Web Token)의 생성, 파싱, 서명 검증을 담당하는 핵심 컴포넌트입니다.
 * 'jjwt' 라이브러리를 사용하여 표준 규격(RFC 7519)에 맞는 토큰을 발급하며,
 * 대칭키 암호화 알고리즘(HMAC-SHA)을 사용하여 서명의 무결성을 보장합니다.
 */
@Component
class JwtProvider(
    @Value("\${jwt.secret}") secretKey: String,
    @Value("\${jwt.access-token-validity}") private val accessTokenValidity: Long,
    @Value("\${jwt.refresh-token-validity}") private val refreshTokenValidity: Long
) {
    private val key: SecretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey))

    /**
     * 인증된 사용자 정보를 기반으로 Access Token과 Refresh Token 쌍을 생성합니다.
     *
     * [Payload 설계 전략]
     * Access Token의 Payload(Claims)에는 'memberId'와 'nickname'을 포함시킵니다.
     * 이는 프론트엔드에서 사용자 정보를 표시할 때 매번 별도의 API(예: /me)를 호출하지 않고,
     * 토큰 디코딩만으로 즉시 UI를 렌더링할 수 있게 하여 네트워크 비용을 절감하기 위함입니다.
     */
    fun generateTokenDto(authentication: Authentication): TokenDto {
        val authorities = authentication.authorities.joinToString(",") { it.authority }
        val now = Date().time

        // 인증 객체에서 비즈니스 도메인 정보 추출 (CustomUserDetails 활용)
        val principal = authentication.principal as CustomUserDetails
        val memberId = principal.memberId
        val nickname = principal.nickname

        // 1. Access Token 생성 (Stateless 인증용, 짧은 유효기간)
        val accessTokenExpiresIn = Date(now + accessTokenValidity)
        val accessToken = Jwts.builder()
            .subject(authentication.name) // 표준 sub claim (Email)
            .claim("auth", authorities)   // 사용자 권한 (ROLE_USER 등)
            .claim("memberId", memberId)  // 프론트엔드 식별 편의성 제공
            .claim("nickname", nickname)  // 프론트엔드 표기 편의성 제공
            .expiration(accessTokenExpiresIn)
            .signWith(key)
            .compact()

        // 2. Refresh Token 생성 (토큰 갱신용, 긴 유효기간)
        // 불필요한 정보 노출을 최소화하기 위해 식별자(sub)와 만료일만 포함
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

    /**
     * Access Token을 복호화하여 Spring Security가 이해할 수 있는 Authentication 객체로 변환합니다.
     * 요청 당 1회 수행되므로 성능을 고려하여 DB 조회 없이 토큰의 Claims만으로 객체를 구성합니다.
     */
    fun getAuthentication(accessToken: String): Authentication {
        val claims = parseClaims(accessToken)

        if (claims["auth"] == null) {
            throw RuntimeException("권한 정보가 없는 토큰입니다.")
        }

        val authorities: Collection<GrantedAuthority> =
            claims["auth"].toString()
                .split(",")
                .map { SimpleGrantedAuthority(it) }

        // UserDetails 객체를 생성하여 Authentication에 담음 (비밀번호는 불필요하므로 빈 문자열)
        val principal = User(claims.subject, "", authorities)
        return UsernamePasswordAuthenticationToken(principal, "", authorities)
    }

    /**
     * 토큰의 유효성을 검증합니다.
     *
     * 서명 위조, 만료, 형식 오류 등 다양한 예외 케이스를 정교하게 catch하여 처리합니다.
     * 필터 레벨에서 호출되므로 false 반환 시 해당 요청은 인증 실패로 간주됩니다.
     */
    fun validateToken(token: String): Boolean {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token)
            return true
        } catch (e: SecurityException) {
            // log.warn("잘못된 JWT 서명입니다.")
        } catch (e: MalformedJwtException) {
            // log.warn("손상된 JWT 토큰입니다.")
        } catch (e: ExpiredJwtException) {
            // log.warn("만료된 JWT 토큰입니다.")
        } catch (e: UnsupportedJwtException) {
            // log.warn("지원되지 않는 JWT 토큰입니다.")
        } catch (e: IllegalArgumentException) {
            // log.warn("JWT 토큰이 비어있거나 잘못되었습니다.")
        }
        return false
    }

    private fun parseClaims(accessToken: String): Claims {
        return try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(accessToken).payload
        } catch (e: ExpiredJwtException) {
            // 만료된 토큰이더라도 Claims 정보(사용자 ID 등)가 필요할 수 있으므로 예외에서 꺼내 반환
            e.claims
        }
    }
}