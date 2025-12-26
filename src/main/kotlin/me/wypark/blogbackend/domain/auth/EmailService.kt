package me.wypark.blogbackend.domain.auth

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit
import kotlin.random.Random

@Service
class EmailService(
    private val javaMailSender: JavaMailSender,
    private val redisTemplate: RedisTemplate<String, String>
) {

    // 인증 코드 전송
    fun sendVerificationCode(email: String) {
        val code = createVerificationCode()

        // 1. Redis에 저장 (Key: "Verify:이메일", Value: 코드, 유효시간: 5분)
        redisTemplate.opsForValue().set(
            "Verify:$email",
            code,
            5,
            TimeUnit.MINUTES
        )

        // 2. 메일 발송
        sendMail(email, code)
    }

    // 인증 코드 검증
    fun verifyCode(email: String, code: String): Boolean {
        val savedCode = redisTemplate.opsForValue().get("Verify:$email")
        return savedCode != null && savedCode == code
    }

    private fun createVerificationCode(): String {
        return Random.nextInt(100000, 999999).toString() // 6자리 난수
    }

    private fun sendMail(email: String, code: String) {
        val mimeMessage = javaMailSender.createMimeMessage()
        val helper = MimeMessageHelper(mimeMessage, "utf-8")

        helper.setTo(email)
        helper.setSubject("[Blog] 회원가입 인증 코드입니다.")

        val htmlContent = """
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
                    <div style="font-size: 24px; font-weight: bold; letter-spacing: 5px; color: #333;">
                        $code
                    </div>
                </div>
                
                <div style="margin-top: 30px; text-align: center;">
                    <p style="font-size: 14px; color: #888;">이 코드는 5분간 유효합니다.</p>
                </div>
            </div>
        """.trimIndent()

        helper.setText(htmlContent, true) // true: HTML 모드 켜기
        javaMailSender.send(mimeMessage)
    }
}