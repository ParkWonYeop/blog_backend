package me.wypark.blogbackend.api.controller

import jakarta.validation.Valid
import me.wypark.blogbackend.api.common.ApiResponse
import me.wypark.blogbackend.api.dto.*
import me.wypark.blogbackend.domain.auth.AuthService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.User
import org.springframework.web.bind.annotation.*

/**
 * [인증/인가 컨트롤러]
 *
 * JWT(Json Web Token) 기반의 Stateless 인증 처리를 담당하는 엔드포인트 집합입니다.
 * 표준적인 Access/Refresh Token 패턴을 사용하며, 보안 강화를 위해
 * Refresh Token Rotation(RTR) 전략을 적용하여 탈취된 토큰의 재사용을 방지합니다.
 */
@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {

    /**
     * 신규 회원 가입을 요청합니다.
     *
     * 봇(Bot)이나 무분별한 가입을 방지하기 위해, 가입 요청 즉시 인증 메일을 발송합니다.
     * 이메일 인증이 완료(`isVerified = true`)되기 전까지는 로그인이 제한됩니다.
     */
    @PostMapping("/signup")
    fun signup(@RequestBody @Valid request: SignupRequest): ResponseEntity<ApiResponse<Nothing>> {
        authService.signup(request)
        return ResponseEntity.ok(ApiResponse.success(message = "회원가입에 성공했습니다. 이메일 인증을 완료해주세요."))
    }

    /**
     * 이메일 인증 코드를 검증하여 계정을 활성화합니다.
     * Redis에 TTL(Time-To-Live)로 저장된 임시 코드와 사용자의 입력을 대조합니다.
     */
    @PostMapping("/verify")
    fun verifyEmail(@RequestBody @Valid request: VerifyEmailRequest): ResponseEntity<ApiResponse<Nothing>> {
        authService.verifyEmail(request.email, request.code)
        return ResponseEntity.ok(ApiResponse.success(message = "이메일 인증이 완료되었습니다."))
    }

    /**
     * 사용자 자격 증명(Email/Password)을 검증하고 토큰 쌍을 발급합니다.
     * 인증 성공 시 Access Token과 Refresh Token이 모두 반환됩니다.
     */
    @PostMapping("/login")
    fun login(@RequestBody @Valid request: LoginRequest): ResponseEntity<ApiResponse<TokenDto>> {
        val tokenDto = authService.login(request)
        return ResponseEntity.ok(ApiResponse.success(tokenDto))
    }

    /**
     * Access Token 만료 시, Refresh Token을 사용하여 토큰을 갱신합니다 (Silent Refresh).
     *
     * [보안 전략: Refresh Token Rotation]
     * 토큰 갱신 시 기존 Refresh Token은 폐기되고 새로운 Refresh Token이 발급됩니다.
     * 만약 이미 폐기된 토큰으로 재요청이 들어올 경우, 탈취된 것으로 간주하여 해당 유저의 모든 토큰을 무효화합니다.
     */
    @PostMapping("/reissue")
    fun reissue(@RequestBody request: ReissueRequest): ResponseEntity<ApiResponse<TokenDto>> {
        val tokenDto = authService.reissue(request.accessToken, request.refreshToken)
        return ResponseEntity.ok(ApiResponse.success(tokenDto))
    }

    /**
     * 로그아웃 처리를 수행합니다.
     *
     * JWT 특성상 클라이언트가 토큰을 삭제하는 것이 기본이지만,
     * 서버 측에서도 Redis에 저장된 Refresh Token을 즉시 삭제(Evict)하여
     * 더 이상 해당 토큰으로 액세스 토큰을 재발급받지 못하도록 차단합니다.
     */
    @PostMapping("/logout")
    fun logout(@AuthenticationPrincipal user: User): ResponseEntity<ApiResponse<Nothing>> {
        authService.logout(user.username) // user.username은 SecurityContext에 저장된 email입니다.
        return ResponseEntity.ok(ApiResponse.success(message = "로그아웃 되었습니다."))
    }
}

// DTO가 다른 곳에서 재사용 않아 응집도를 위해 같은 파일 내에 정의
data class ReissueRequest(val accessToken: String, val refreshToken: String)