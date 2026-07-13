package me.wypark.blogbackend.application.auth

interface RefreshTokenStore {
    fun save(email: String, refreshToken: String)
    fun findByEmail(email: String): String?
    fun delete(email: String)
}

interface EmailVerification {
    fun sendVerificationCode(email: String)
    fun verifyCode(email: String, code: String): Boolean
}
