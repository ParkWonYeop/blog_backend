package me.wypark.blogbackend.infrastructure.auth

import me.wypark.blogbackend.application.auth.EmailVerification
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

@Service
class RedisEmailVerification(
    private val mailSender: JavaMailSender,
    private val redisTemplate: RedisTemplate<String, String>
) : EmailVerification {

    override fun sendVerificationCode(email: String) {
        val code = verificationCode()
        redisTemplate.opsForValue().set(
            key(email),
            code,
            CODE_TTL_MINUTES,
            TimeUnit.MINUTES
        )
        sendMail(email, code)
    }

    override fun verifyCode(email: String, code: String): Boolean {
        return redisTemplate.opsForValue().get(key(email)) == code
    }

    private fun verificationCode(): String {
        return (random.nextInt(CODE_RANGE) + MINIMUM_CODE).toString()
    }

    private fun sendMail(email: String, code: String) {
        val message = mailSender.createMimeMessage()
        MimeMessageHelper(message, "utf-8").apply {
            setTo(email)
            setSubject("[Blog] 회원가입 인증 코드입니다.")
            setText(emailContent(code), true)
        }
