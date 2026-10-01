package me.wypark.blogbackend.domain.auth.dto

data class TokenDto(
    val grantType: String = "Bearer",
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiresIn: Long
) {
    fun toResponse() = TokenResponse(
        grantType = grantType,
        accessToken = accessToken,
        accessTokenExpiresIn = accessTokenExpiresIn
    )
}

/** Refresh tokens are delivered only through the httpOnly cookie. */
data class TokenResponse(
    val grantType: String,
    val accessToken: String,
    val accessTokenExpiresIn: Long
)
