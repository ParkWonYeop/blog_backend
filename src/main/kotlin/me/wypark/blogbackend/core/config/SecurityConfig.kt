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

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val corsFilter: CorsFilter,
    private val jwtAuthenticationFilter: JwtAuthenticationFilter // 주입 추가
) {

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
            .authorizeHttpRequests { auth ->
                auth.requestMatchers("/api/auth/**").permitAll()
                auth.requestMatchers(HttpMethod.GET, "/api/posts/**", "/api/categories/**", "/api/tags/**").permitAll()
                auth.requestMatchers("/api/comments/**").permitAll() // 비회원 댓글 허용
                auth.requestMatchers(HttpMethod.GET, "/api/profile").permitAll()
                auth.requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")
                auth.anyRequest().authenticated()
            }
            // 필터 등록: UsernamePasswordAuthenticationFilter 앞에 JwtFilter를 실행
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }
}