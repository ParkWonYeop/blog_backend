package me.wypark.blogbackend.domain.auth

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * [이메일 인증 서비스]
 *
 * 회원가입 시 본인 확인을 위한 OTP(One Time Password) 발송 및 검증 로직을 담당합니다.
 *
 * [아키텍처 설계]
 * 인증 코드의 상태(State) 관리를 위해 인메모리 DB인 Redis를 사용합니다.
 * RDB를 사용하지 않음으로써 만료된 코드의 정리(Cleanup) 비용을 없애고, 빠른 액세스 속도를 보장합니다.
 */
@Service
class EmailService(
    private val javaMailSender: JavaMailSender,
    private val redisTemplate: RedisTemplate<String, String>
) {

    /**
     * 인증 코드를 생성하고 이메일로 발송합니다.
     *
     * 생성된 코드는 Redis에 저장되며, 보안을 위해 짧은 유효시간(TTL)을 가집니다.
     * 이메일 발송은 외부 SMTP 서버를 이용하므로, 트래픽 급증 시 비동기 큐(RabbitMQ/Kafka) 도입을 고려할 수 있습니다.
     */
    fun sendVerificationCode(email: String) {
        val code = createVerificationCode()

        // Redis 저장 전략: Key에 Prefix("Verify:")를 붙여 네임스페이스를 구분하고,
        // 5분의 TTL(Time-To-Live)을 설정하여 별도의 삭제 로직 없이 자동 만료되도록 처리함.
        redisTemplate.opsForValue().set(
            "Verify:$email",
            code,
            5,
            TimeUnit.MINUTES
        )

        sendMail(email, code)
    }

    /**
     * 사용자가 입력한 코드와 Redis에 저장된 원본 코드를 대조합니다.
     * 코드가 만료되었거나 일치하지 않을 경우 false를 반환합니다.
     */
    fun verifyCode(email: String, code: String): Boolean {
        val savedCode = redisTemplate.opsForValue().get("Verify:$email")
        return savedCode != null && savedCode == code
    }

    /**
     * 6자리 숫자(100000 ~ 999999)로 구성된 난수를 생성합니다.
     * 보안성과 사용자 입력 편의성(Usability) 사이의 균형을 맞춘 길이입니다.
     */
    private fun createVerificationCode(): String {
        return Random.nextInt(100000, 999999).toString()
    }

    /**
     * HTML 템플릿을 사용하여 인증 메일을 발송합니다.
     * 단순 텍스트보다 신뢰감을 주고 브랜드 아이덴티티를 전달하기 위해 인라인 스타일(CSS)을 적용했습니다.
     */
    private fun sendMail(email: String, code: String) {
        val mimeMessage = javaMailSender.createMimeMessage()
        val helper = MimeMessageHelper(mimeMessage, "utf-8")

        helper.setTo(email)
        helper.setSubject("[Blog] 회원가입 인증 코드입니다.")

        // HTML 본문 구성 (이메일 클라이언트 호환성을 위해 Inline CSS 사용 권장)
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

        helper.setText(htmlContent, true) // true: HTML 모드 활성화
        javaMailSender.send(mimeMessage)
    }
}