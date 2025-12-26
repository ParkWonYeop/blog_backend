package me.wypark.blogbackend.api.controller

import jakarta.validation.Valid
import me.wypark.blogbackend.api.common.ApiResponse
import me.wypark.blogbackend.api.dto.*
import me.wypark.blogbackend.domain.auth.AuthService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.User
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/signup")
    fun signup(@RequestBody @Valid request: SignupRequest): ResponseEntity<ApiResponse<Nothing>> {
        authService.signup(request)
        return ResponseEntity.ok(ApiResponse.success(message = "회원가입에 성공했습니다. 이메일 인증을 완료해주세요."))
    }

    @PostMapping("/verify")
    fun verifyEmail(@RequestBody @Valid request: VerifyEmailRequest): ResponseEntity<ApiResponse<Nothing>> {
        authService.verifyEmail(request.email, request.code)
        return ResponseEntity.ok(ApiResponse.success(message = "이메일 인증이 완료되었습니다."))
    }

    @PostMapping("/login")
    fun login(@RequestBody @Valid request: LoginRequest): ResponseEntity<ApiResponse<TokenDto>> {
        val tokenDto = authService.login(request)
        return ResponseEntity.ok(ApiResponse.success(tokenDto))
    }

    @PostMapping("/reissue")
    fun reissue(@RequestBody request: ReissueRequest): ResponseEntity<ApiResponse<TokenDto>> {
        val tokenDto = authService.reissue(request.accessToken, request.refreshToken)
        return ResponseEntity.ok(ApiResponse.success(tokenDto))
    }

    @PostMapping("/logout")
    fun logout(@AuthenticationPrincipal user: User): ResponseEntity<ApiResponse<Nothing>> {
        authService.logout(user.username) // user.username은 email입니다.
        return ResponseEntity.ok(ApiResponse.success(message = "로그아웃 되었습니다."))
    }
}

data class ReissueRequest(val accessToken: String, val refreshToken: String)