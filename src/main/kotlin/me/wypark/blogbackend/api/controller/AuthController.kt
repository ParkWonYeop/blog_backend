package me.wypark.blogbackend.api.controller

import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import me.wypark.blogbackend.api.common.ApiResponse
import me.wypark.blogbackend.application.auth.AuthService
import me.wypark.blogbackend.application.auth.LoginRequest
import me.wypark.blogbackend.application.auth.SignupRequest
import me.wypark.blogbackend.application.auth.TokenDto
import me.wypark.blogbackend.application.auth.VerifyEmailRequest
import me.wypark.blogbackend.application.common.BusinessException
import me.wypark.blogbackend.core.config.JwtProperties
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.User
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
    ): ResponseEntity<ApiResponse<TokenDto>> {
        val tokenDto = authService.login(request)
        setRefreshTokenCookie(response, tokenDto.refreshToken)
        return ResponseEntity.ok(ApiResponse.success(tokenDto))
    }

    @PostMapping("/reissue")
    fun reissue(
        @CookieValue(REFRESH_TOKEN_COOKIE, required = false) cookieToken: String?,
        @RequestBody(required = false) request: ReissueRequest?,
        response: HttpServletResponse
    ): ResponseEntity<ApiResponse<TokenDto>> {
        // 쿠키 우선, body는 구버전 클라이언트 호환용.
        val refreshToken = cookieToken ?: request?.refreshToken
            ?: throw BusinessException("Refresh Token이 없습니다.")
        val tokenDto = authService.reissue(refreshToken)
        setRefreshTokenCookie(response, tokenDto.refreshToken)
        return ResponseEntity.ok(ApiResponse.success(tokenDto))
    }

    @PostMapping("/logout")
    fun logout(
        @AuthenticationPrincipal user: User,
        response: HttpServletResponse
    ): ResponseEntity<ApiResponse<Nothing>> {
        authService.logout(user.username)
        setRefreshTokenCookie(response, "", maxAge = Duration.ZERO)
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
            .sameSite("Lax")
            .path("/api/auth")
            .maxAge(maxAge)
            .build()
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())
    }

    companion object {
        private const val REFRESH_TOKEN_COOKIE = "refreshToken"
    }
}

// accessToken은 구버전 클라이언트 호환용으로만 받고 사용하지 않는다.
data class ReissueRequest(val accessToken: String? = null, val refreshToken: String? = null)
