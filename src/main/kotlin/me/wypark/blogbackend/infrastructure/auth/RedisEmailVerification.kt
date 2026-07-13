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
        mailSender.send(message)
    }

    private fun emailContent(code: String): String {
        return """
            <div style="font-family: 'Apple SD Gothic Neo', 'sans-serif' !important; width: 540px; height: 600px; border-top: 4px solid #00C73C; margin: 100px auto; padding: 30px 0; box-sizing: border-box;">
                <h1 style="margin: 0; padding: 0 5px; font-size: 28px; font-weight: 400;">
                    <span style="font-size: 15px; margin: 0 0 10px 3px;">Wypark Blog</span><br />
                    <span style="color: #00C73C;">메일인증</span> 안내입니다.
                </h1>
                <p style="font-size: 16px; line-height: 26px; margin-top: 50px; padding: 0 5px;">
                    안녕하세요.<br />
                    Wypark Blog에 가입해 주셔서 진심으로 감사드립니다.<br />
                    아래 <b style="color: #00C73C;">'인증 코드'</b>를 입력하여 회원가입을 완료해 주세요.<br />
                    감사합니다.
                </p>
                <div style="margin-top: 50px; border-top: 1px solid #DDD; border-bottom: 1px solid #DDD; padding: 25px; background-color: #F9F9F9; text-align: center;">
                    <div style="font-size: 24px; font-weight: bold; letter-spacing: 5px; color: #333;">$code</div>
                </div>
                <div style="margin-top: 30px; text-align: center;">
                    <p style="font-size: 14px; color: #888;">이 코드는 5분간 유효합니다.</p>
                </div>
            </div>
        """.trimIndent()
    }

    private fun key(email: String): String = "$KEY_PREFIX$email"

    companion object {
        private const val KEY_PREFIX = "Verify:"
        private const val CODE_TTL_MINUTES = 5L
        private const val MINIMUM_CODE = 100_000
        private const val CODE_RANGE = 899_999
        private val random = SecureRandom()
    }
}
