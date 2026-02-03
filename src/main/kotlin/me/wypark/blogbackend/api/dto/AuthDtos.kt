package me.wypark.blogbackend.api.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * [회원가입 요청 DTO]
 *
 * 사용자 등록을 위한 데이터 전송 객체입니다.
 * Controller 진입 시점(@Valid)에서 입력값의 형식 검증을 수행하여,
 * 비즈니스 로직(Service) 단계에서의 불필요한 연산을 방지합니다 (Fail-Fast 전략).
 */
data class SignupRequest(
    @field:NotBlank(message = "이메일은 필수입니다.")
    @field:Email(message = "올바른 이메일 형식이 아닙니다.")
    val email: String,

    /**
     * 비밀번호 복잡도 정책: 최소 8자 ~ 최대 20자
     *
     * Note:
     * 이 필드는 클라이언트로부터 평문(Plain Text)으로 전달되므로,
     * 전송 구간 암호화(HTTPS/TLS)가 보장된 환경에서만 사용되어야 합니다.
     * DB 저장 시에는 반드시 단방향 해시 함수(BCrypt 등)를 통해 암호화됩니다.
     */
    @field:NotBlank(message = "비밀번호는 필수입니다.")
    @field:Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하로 입력해주세요.")
    val password: String,

    @field:NotBlank(message = "닉네임은 필수입니다.")
    @field:Size(min = 2, max = 10, message = "닉네임은 2자 이상 10자 이하로 입력해주세요.")
    val nickname: String
)

/**
 * [로그인 요청 DTO]
 *
 * JWT 토큰 발급을 위한 사용자 자격 증명(Credentials)을 전달받는 객체입니다.
 */
data class LoginRequest(
    @field:NotBlank(message = "이메일을 입력해주세요.")
    val email: String,

    @field:NotBlank(message = "비밀번호를 입력해주세요.")
    val password: String
)

/**
 * [이메일 인증 확인 DTO]
 *
 * 회원가입 직후 발송된 OTP(One Time Password) 코드를 검증하기 위한 요청 객체입니다.
 * 이메일 소유권 확인(Proof of Ownership)을 위해 사용됩니다.
 */
data class VerifyEmailRequest(
    @field:NotBlank(message = "이메일을 입력해주세요")
    val email: String,

    @field:NotBlank(message = "인증 코드를 입력해주세요")
    val code: String
)