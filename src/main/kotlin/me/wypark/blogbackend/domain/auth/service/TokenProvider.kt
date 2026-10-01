package me.wypark.blogbackend.domain.auth.service
import me.wypark.blogbackend.domain.auth.dto.TokenDto

import org.springframework.security.core.Authentication

interface TokenProvider {
    fun generate(authentication: Authentication): TokenDto
    fun isValidAccessToken(token: String): Boolean
    fun isValidRefreshToken(token: String): Boolean
    fun extractSubject(token: String): String
}
