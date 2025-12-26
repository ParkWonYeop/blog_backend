package me.wypark.blogbackend.api.dto

data class TokenDto(
    val grantType: String = "Bearer",
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiresIn: Long
)