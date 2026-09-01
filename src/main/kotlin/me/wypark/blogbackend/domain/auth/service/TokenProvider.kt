package me.wypark.blogbackend.domain.auth.service
import me.wypark.blogbackend.domain.auth.dto.TokenDto

import org.springframework.security.core.Authentication

interface TokenProvider {
    fun generate(authentication: Authentication): TokenDto
    fun isValid(token: String): Boolean
    fun extractSubject(token: String): String
}
