package me.wypark.blogbackend.api.dto

/**
 * [JWT 토큰 응답 DTO]
 *
 * 로그인 또는 토큰 재발급 성공 시 클라이언트에게 반환되는 인증 정보 객체입니다.
 * RFC 6750 (Bearer Token Usage) 표준을 따르며, 클라이언트가 인증 헤더(Authorization)를
 * 올바르게 구성할 수 있도록 필요한 메타데이터를 함께 제공합니다.
 *
 * @property grantType 인증 타입 (Default: "Bearer")
 * @property accessToken 리소스 접근을 위한 단기 유효 토큰 (Stateless)
 * @property refreshToken Access Token 갱신을 위한 장기 유효 토큰 (Rotation 전략 적용)
 * @property accessTokenExpiresIn Access Token의 유효 기간(ms). 클라이언트가 만료 시점을 예측하여 미리 갱신 요청을 보낼 수 있도록 함.
 */
data class TokenDto(
    val grantType: String = "Bearer",
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiresIn: Long
)