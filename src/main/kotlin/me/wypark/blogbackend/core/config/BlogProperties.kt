package me.wypark.blogbackend.core.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("jwt")
data class JwtProperties(
    val secret: String,
    val accessTokenValidity: Long,
    val refreshTokenValidity: Long
)

@ConfigurationProperties("blog.cors")
data class CorsProperties(
    val allowedOrigins: List<String> = listOf("https://blog.wypark.me")
)

