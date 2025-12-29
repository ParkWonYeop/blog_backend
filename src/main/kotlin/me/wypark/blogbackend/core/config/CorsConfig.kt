package me.wypark.blogbackend.core.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter

@Configuration
class CorsConfig {
    @Bean
    fun corsFilter(): CorsFilter {
        val source = UrlBasedCorsConfigurationSource()
        val config = CorsConfiguration()

        config.allowCredentials = true
        config.addAllowedOrigin("https://blog.wypark.me") // 프론트 도메인
        config.addAllowedHeader("*") // 클라이언트가 보내는 모든 헤더 허용 (Authorization 포함)
        config.addAllowedMethod("*")

        // [중요] 클라이언트가 응답 헤더에서 'Authorization'이나 커스텀 토큰 헤더를 읽을 수 있게 허용
        config.addExposedHeader("Authorization")
        config.addExposedHeader("Refresh-Token") // 리프레시 토큰도 헤더로 준다면 추가

        source.registerCorsConfiguration("/api/**", config)
        return CorsFilter(source)
    }
}

