package me.wypark.blogbackend.core.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter

/**
 * [CORS(Cross-Origin Resource Sharing) 설정]
 *
 * 프론트엔드(React/Next.js)와 백엔드(Spring Boot)의 도메인이 다를 경우 발생하는
 * 브라우저의 보안 제약(SOP)을 해결하기 위한 설정입니다.
 *
 * 단순한 와일드카드(*) 허용이 아닌, 신뢰할 수 있는 특정 도메인(Origin)에 대해서만
 * 리소스 접근 권한을 명시적으로 부여하여 보안성을 확보했습니다.
 */
@Configuration
class CorsConfig {

    @Bean
    fun corsFilter(): CorsFilter {
        val source = UrlBasedCorsConfigurationSource()
        val config = CorsConfiguration()

        // 1. 인증 정보(Cookie, Authorization Header) 포함 허용
        // 이 옵션을 true로 설정하면, 보안상 addAllowedOrigin에 와일드카드(*)를 사용할 수 없습니다.
        config.allowCredentials = true

        // 2. 신뢰할 수 있는 출처(Origin) 명시
        // 로컬 개발 환경과 배포 환경(Production)의 도메인을 각각 등록합니다.
        config.addAllowedOrigin("https://blog.wypark.me")
        config.addAllowedOrigin("http://localhost:3000")
        config.addAllowedOrigin("http://localhost:5173")
        // config.addAllowedOrigin("http://localhost:3000") // 로컬 테스트 시 주석 해제

        // 3. 허용할 HTTP 메서드 및 헤더
        // REST API의 유연성을 위해 모든 표준 메서드와 헤더를 허용합니다.
        config.addAllowedHeader("*")
        config.addAllowedMethod("*")

        // 4. [중요] 응답 헤더 노출 설정 (Expose Headers)
        // 브라우저는 기본적으로 보안상 CORS 요청에 대한 응답 헤더 중 일부(Cache-Control, Content-Type 등)만 JavaScript에서 접근하도록 제한합니다.
        // 따라서, 클라이언트가 로그인 후 발급된 JWT 토큰(Authorization)을 읽을 수 있도록 명시적으로 노출시켜야 합니다.
        config.addExposedHeader("Authorization")
        config.addExposedHeader("Refresh-Token")

        source.registerCorsConfiguration("/api/**", config)
        return CorsFilter(source)
    }
}
