package me.wypark.blogbackend.domain.auth.service

interface RefreshTokenStore {
    fun save(email: String, refreshToken: String)
    fun findByEmail(email: String): String?
    fun delete(email: String)
}

interface EmailVerification {
    fun sendVerificationCode(email: String)
    fun verifyCode(email: String, code: String): Boolean
}
