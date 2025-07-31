package me.wypark.blogbackend.application.auth

import org.springframework.security.core.Authentication

interface TokenProvider {
    fun generate(authentication: Authentication): TokenDto
    fun isValid(token: String): Boolean
    fun extractSubject(token: String): String
}
