package me.wypark.blogbackend.core.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import java.util.*

/**
 * [메일 발송 설정]
 *
 * 회원가입 인증 코드 발송 등을 위한 SMTP(Simple Mail Transfer Protocol) 서버 설정입니다.
 * Google Gmail SMTP 등을 사용하여 외부 메일 서버와 연동하며,
 * 네트워크 지연이나 연결 실패 시 스레드가 차단(Blocking)되는 것을 방지하기 위한 타임아웃 설정이 포함되어 있습니다.
 */
@Configuration
class MailConfig(
    @Value("\${spring.mail.host}") private val host: String,
    @Value("\${spring.mail.port}") private val port: Int,
    @Value("\${spring.mail.username}") private val username: String,
    @Value("\${spring.mail.password}") private val password: String,
    @Value("\${spring.mail.properties.mail.smtp.auth}") private val auth: String,
    @Value("\${spring.mail.properties.mail.smtp.starttls.enable}") private val starttlsEnable: String,
    @Value("\${spring.mail.properties.mail.smtp.starttls.required}") private val starttlsRequired: String,
    @Value("\${spring.mail.properties.mail.smtp.connectiontimeout}") private val connectionTimeout: Int,
    @Value("\${spring.mail.properties.mail.smtp.timeout}") private val timeout: Int,
    @Value("\${spring.mail.properties.mail.smtp.writetimeout}") private val writeTimeout: Int
) {

    /**
     * JavaMailSender 빈 등록
     *
     * Spring Mail 라이브러리의 핵심 인터페이스 구현체를 생성합니다.
     * application.yml에서 주입받은 환경 변수들을 기반으로 SMTP 연결을 초기화합니다.
     */
    @Bean
    fun javaMailSender(): JavaMailSender {
        val mailSender = JavaMailSenderImpl()

        // 기본 SMTP 서버 정보 설정
        mailSender.host = host
        mailSender.port = port
        mailSender.username = username
        mailSender.password = password
        mailSender.defaultEncoding = "UTF-8"

        // JavaMail 세부 속성 설정
        val props: Properties = mailSender.javaMailProperties
        props["mail.transport.protocol"] = "smtp"
        props["mail.smtp.auth"] = auth
        props["mail.smtp.starttls.enable"] = starttlsEnable
        props["mail.smtp.starttls.required"] = starttlsRequired

        // [디버깅 설정]
        // 개발 환경에서는 메일 발송 로그를 상세히 확인하기 위해 true로 설정합니다.
        // 운영(Prod) 환경에서는 로그 양이 많아질 수 있으므로 false로 변경하거나 로그 레벨을 조정해야 합니다.
        props["mail.debug"] = "true"

        // [안정성 설정: 타임아웃]
        // 외부 SMTP 서버 응답이 지연될 경우, 애플리케이션 스레드가 무한 대기(Hang) 상태에 빠지는 것을 방지합니다.
        // 각각 연결(Connection), 읽기(Read), 쓰기(Write) 타임아웃을 명시하여 Fail-Fast를 유도합니다.
        props["mail.smtp.connectiontimeout"] = connectionTimeout
        props["mail.smtp.timeout"] = timeout
        props["mail.smtp.writetimeout"] = writeTimeout

        return mailSender
    }
}