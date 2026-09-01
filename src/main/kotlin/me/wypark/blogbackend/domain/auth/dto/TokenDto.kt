package me.wypark.blogbackend.domain.auth.dto

data class TokenDto(
    val grantType: String = "Bearer",
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiresIn: Long
)
