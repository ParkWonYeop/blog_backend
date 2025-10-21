package me.wypark.blogbackend.core.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter

@Configuration
class CorsConfig(
    private val properties: CorsProperties
) {

    @Bean
    fun corsFilter(): CorsFilter {
        val source = UrlBasedCorsConfigurationSource()
        val config = CorsConfiguration()

        config.allowCredentials = true

        properties.allowedOrigins.forEach(config::addAllowedOrigin)

        config.addAllowedHeader("*")
        config.addAllowedMethod("*")

        config.addExposedHeader("Authorization")
        config.addExposedHeader("Refresh-Token")

        source.registerCorsConfiguration("/api/**", config)
        return CorsFilter(source)
    }
}
