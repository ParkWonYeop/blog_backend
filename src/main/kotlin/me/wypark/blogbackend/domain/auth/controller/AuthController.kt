package me.wypark.blogbackend.domain.auth.controller

import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import me.wypark.blogbackend.global.common.ApiResponse
import me.wypark.blogbackend.domain.auth.service.AuthService
import me.wypark.blogbackend.domain.auth.dto.LoginRequest
import me.wypark.blogbackend.domain.auth.dto.SignupRequest
import me.wypark.blogbackend.domain.auth.dto.TokenResponse
import me.wypark.blogbackend.domain.auth.dto.VerifyEmailRequest
import me.wypark.blogbackend.global.common.BusinessException
import me.wypark.blogbackend.global.config.JwtProperties
import me.wypark.blogbackend.global.security.AuthenticatedUser
import org.springframework.http.CacheControl
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Duration

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
    private val jwtProperties: JwtProperties
) {

    @PostMapping("/signup")
    fun signup(@RequestBody @Valid request: SignupRequest): ResponseEntity<ApiResponse<Nothing>> {
        authService.signup(request)
        return ResponseEntity.ok(
            ApiResponse.success(message = "회원가입에 성공했습니다. 이메일 인증을 완료해주세요.")
        )
    }

    @PostMapping("/verify")
    fun verifyEmail(@RequestBody @Valid request: VerifyEmailRequest): ResponseEntity<ApiResponse<Nothing>> {
        authService.verifyEmail(request.email, request.code)
        return ResponseEntity.ok(ApiResponse.success(message = "이메일 인증이 완료되었습니다."))
    }

    @PostMapping("/login")
    fun login(
        @RequestBody @Valid request: LoginRequest,
        response: HttpServletResponse
    ): ResponseEntity<ApiResponse<TokenResponse>> {
        val tokenDto = authService.login(request)
        setRefreshTokenCookie(response, tokenDto.refreshToken)
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .body(ApiResponse.success(tokenDto.toResponse()))
    }

    @PostMapping("/reissue")
    fun reissue(
        @CookieValue(REFRESH_TOKEN_COOKIE, required = false) cookieToken: String?,
        response: HttpServletResponse
    ): ResponseEntity<ApiResponse<TokenResponse>> {
        val refreshToken = cookieToken ?: throw BusinessException("Refresh Token이 없습니다.")
        val tokenDto = authService.reissue(refreshToken)
        setRefreshTokenCookie(response, tokenDto.refreshToken)
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .body(ApiResponse.success(tokenDto.toResponse()))
    }

    @PostMapping("/logout")
    fun logout(
        @AuthenticationPrincipal user: AuthenticatedUser,
        response: HttpServletResponse
    ): ResponseEntity<ApiResponse<Nothing>> {
        try {
            authService.logout(user.username)
        } finally {
            setRefreshTokenCookie(response, "", maxAge = Duration.ZERO)
        }
        return ResponseEntity.ok(ApiResponse.success(message = "로그아웃 되었습니다."))
    }

    private fun setRefreshTokenCookie(
        response: HttpServletResponse,
        token: String,
        maxAge: Duration = Duration.ofMillis(jwtProperties.refreshTokenValidity)
    ) {
        val cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, token)
            .httpOnly(true)
            .secure(true)
            .sameSite("Strict")
            .path("/api/auth")
            .maxAge(maxAge)
            .build()
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())
    }

    companion object {
        private const val REFRESH_TOKEN_COOKIE = "refreshToken"
    }
}
