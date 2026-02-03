package me.wypark.blogbackend.core.config.jwt

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.util.StringUtils
import org.springframework.web.filter.OncePerRequestFilter

/**
 * [JWT 인증 필터]
 *
 * 모든 HTTP 요청의 헤더를 가로채어 JWT 토큰의 유효성을 검증하는 커스텀 필터입니다.
 * Spring Security의 FilterChain 앞단에 배치되어, 인증된 사용자일 경우
 * SecurityContext에 Authentication 객체를 주입(Populate)하는 역할을 수행합니다.
 */
@Component
class JwtAuthenticationFilter(
    private val jwtProvider: JwtProvider
) : OncePerRequestFilter() {

    /**
     * 필터링 로직 수행
     *
     * [흐름 제어 전략]
     * 토큰이 없거나 유효하지 않더라도 이 필터에서 즉시 예외를 발생시키거나 요청을 차단하지 않습니다.
     * 검증에 실패하면 SecurityContext가 비어있는 상태로 다음 필터(Chain)로 넘어가며,
     * 최종적으로 FilterSecurityInterceptor(SecurityConfig) 단계에서 접근 권한을 판단하게 됩니다.
     * (예: 인증되지 않은 사용자가 /api/public 접근 시 -> 허용, /api/admin 접근 시 -> 403 Forbidden)
     */
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val token = resolveToken(request)

        // 토큰 유효성 검증 및 SecurityContext 설정
        // (Stateless 아키텍처이므로 세션이 아닌 Context에 매 요청마다 인증 정보를 주입합니다)
        if (StringUtils.hasText(token) && jwtProvider.validateToken(token!!)) {
            val authentication = jwtProvider.getAuthentication(token)
            SecurityContextHolder.getContext().authentication = authentication
        }

        // 다음 필터로 진행
        filterChain.doFilter(request, response)
    }

    /**
     * Request Header에서 표준 Bearer 스키마(RFC 6750)를 준수하는 토큰 문자열을 파싱합니다.
     */
    private fun resolveToken(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader(AUTHORIZATION_HEADER)
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(7) // "Bearer " 접두어 제거
        }
        return null
    }

    companion object {
        const val AUTHORIZATION_HEADER = "Authorization"
        const val BEARER_PREFIX = "Bearer "
    }
}