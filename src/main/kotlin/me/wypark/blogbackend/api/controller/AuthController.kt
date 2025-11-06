package me.wypark.blogbackend.api.controller

import jakarta.validation.Valid
import me.wypark.blogbackend.api.common.ApiResponse
import me.wypark.blogbackend.application.auth.AuthService
import me.wypark.blogbackend.application.auth.LoginRequest
import me.wypark.blogbackend.application.auth.SignupRequest
import me.wypark.blogbackend.application.auth.TokenDto
import me.wypark.blogbackend.application.auth.VerifyEmailRequest
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.User
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/signup")
    fun signup(@RequestBody @Valid request: SignupRequest): ResponseEntity<ApiResponse<Nothing>> {
        authService.signup(request)
        return ResponseEntity.ok(
            ApiResponse.success(message = "회원가입에 성공했습니다. 이메일 인증을 완료해주세요.")
        )
    }
