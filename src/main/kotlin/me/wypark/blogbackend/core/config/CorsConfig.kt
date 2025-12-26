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

        config.allowCredentials = true // 쿠키/토큰 허용
        config.addAllowedOriginPattern("*") // 개발용 (배포 시 프론트 도메인으로 변경 추천)
        config.addAllowedHeader("*")
        config.addAllowedMethod("*") // GET, POST, PUT, DELETE 등 모두 허용

        source.registerCorsConfiguration("/api/**", config)
        return CorsFilter(source)
    }
}