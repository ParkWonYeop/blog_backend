package me.wypark.blogbackend.application.auth

data class TokenDto(
    val grantType: String = "Bearer",
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiresIn: Long
)
