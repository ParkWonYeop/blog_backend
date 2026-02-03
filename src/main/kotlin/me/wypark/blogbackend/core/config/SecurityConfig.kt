package me.wypark.blogbackend.core.config

import me.wypark.blogbackend.core.config.jwt.JwtAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.filter.CorsFilter

/**
 * [Spring Security 설정]
 *
 * 애플리케이션의 보안 인가(Authorization) 및 인증(Authentication) 전략을 정의합니다.
 * 전통적인 세션(Session) 기반 인증 대신, REST API 환경에 적합한 JWT(Token) 기반의
 * 무상태(Stateless) 아키텍처를 구현했습니다.
 */
@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val corsFilter: CorsFilter,
    private val jwtAuthenticationFilter: JwtAuthenticationFilter
) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            // 1. 기본 보안 설정 비활성화 (REST API 환경)
            // CSRF(Cross-Site Request Forgery): 쿠키 기반의 세션 인증을 사용하지 않으므로 비활성화 (Header에 토큰을 담아 보냄)
            .csrf { it.disable() }
            // HttpBasic / FormLogin: UI 기반의 인증 창을 사용하지 않으므로 비활성화
            .httpBasic { it.disable() }
            .formLogin { it.disable() }

            // 2. 커스텀 필터 등록
            // CorsFilter: 브라우저의 SOP(Same-Origin Policy) 우회를 위한 설정 적용
            .addFilter(corsFilter)

            // 3. 세션 정책 설정 (Stateless)
            // 서버가 클라이언트의 상태(Session)를 보존하지 않음 -> 서버 확장성(Scale-out) 유리
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }

            // 4. URL별 접근 권한 관리 (인가)
            // Principle of Least Privilege(최소 권한의 원칙)에 따라, 명시적으로 허용된 경로 외에는 모두 인증을 요구
            .authorizeHttpRequests { auth ->
                // 인증 관련(로그인, 회원가입) 및 정적 리소스는 누구나 접근 가능
                auth.requestMatchers("/api/auth/**").permitAll()

                // 조회(Read) 작업은 비회원에게도 허용 (GET 메서드 한정)
                auth.requestMatchers(HttpMethod.GET, "/api/posts/**", "/api/categories/**", "/api/tags/**").permitAll()
                auth.requestMatchers(HttpMethod.GET, "/api/profile").permitAll()

                // 댓글 API: 비회원 작성/삭제도 지원하므로 전체 허용 (내부 로직에서 비밀번호 검증)
                auth.requestMatchers("/api/comments/**").permitAll()

                // 관리자 영역: ROLE_ADMIN 권한을 가진 토큰 소유자만 접근 가능
                auth.requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")

                // 그 외 모든 요청은 인증 필요
                auth.anyRequest().authenticated()
            }
            // 5. JWT 인증 필터 삽입
            // UsernamePasswordAuthenticationFilter(기본 로그인 처리)보다 먼저 실행되어야 함
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }
}