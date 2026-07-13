package me.wypark.blogbackend.core.config

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletResponse
import me.wypark.blogbackend.api.common.ApiResponse
import me.wypark.blogbackend.core.config.jwt.JwtAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.filter.CorsFilter
import java.nio.charset.StandardCharsets

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val corsFilter: CorsFilter,
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val objectMapper: ObjectMapper
) {

    @Bean
    fun authenticationManager(configuration: AuthenticationConfiguration): AuthenticationManager {
        return configuration.authenticationManager
    }

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }

            .addFilter(corsFilter)

            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .exceptionHandling { exceptions ->
                exceptions.authenticationEntryPoint { _, response, _ ->
                    writeErrorResponse(
                        response = response,
                        status = HttpStatus.UNAUTHORIZED,
                        code = "UNAUTHORIZED",
                        message = "인증이 필요합니다."
                    )
                }
                exceptions.accessDeniedHandler { _, response, _ ->
                    writeErrorResponse(
                        response = response,
                        status = HttpStatus.FORBIDDEN,
                        code = "FORBIDDEN",
                        message = "관리자 권한이 필요합니다."
                    )
                }
            }

            .authorizeHttpRequests { auth ->
                auth.requestMatchers("/api/auth/**").permitAll()

                auth.requestMatchers(
                    HttpMethod.GET,
                    "/api/posts/**",
                    "/api/categories/**",
                    "/api/tags/**",
                    "/api/chess-puzzles/**"
                ).permitAll()
                auth.requestMatchers(HttpMethod.GET, "/api/profile").permitAll()

                auth.requestMatchers("/api/comments/**").permitAll()

                auth.requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")

                auth.anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    private fun writeErrorResponse(
        response: HttpServletResponse,
        status: HttpStatus,
        code: String,
        message: String
    ) {
        response.status = status.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = StandardCharsets.UTF_8.name()
        response.writer.write(objectMapper.writeValueAsString(ApiResponse.error(message, code)))
    }
}
