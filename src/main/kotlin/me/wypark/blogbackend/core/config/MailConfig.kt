package me.wypark.blogbackend.core.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import java.util.*

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

    @Bean
    fun javaMailSender(): JavaMailSender {
        val mailSender = JavaMailSenderImpl()

        // 기본 설정
        mailSender.host = host
        mailSender.port = port
        mailSender.username = username
        mailSender.password = password
        mailSender.defaultEncoding = "UTF-8"

        // 세부 프로퍼티 설정
        val props: Properties = mailSender.javaMailProperties
        props["mail.transport.protocol"] = "smtp"
        props["mail.smtp.auth"] = auth
        props["mail.smtp.starttls.enable"] = starttlsEnable
        props["mail.smtp.starttls.required"] = starttlsRequired
        props["mail.debug"] = "true" // 디버깅용 로그 출력 (배포 시 false로 변경 추천)

        // 타임아웃 설정 (서버 응답 없을 때 무한 대기 방지)
        props["mail.smtp.connectiontimeout"] = connectionTimeout
        props["mail.smtp.timeout"] = timeout
        props["mail.smtp.writetimeout"] = writeTimeout

        return mailSender
    }
}