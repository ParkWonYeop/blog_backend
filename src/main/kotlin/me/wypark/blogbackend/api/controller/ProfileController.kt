package me.wypark.blogbackend.api.controller

import me.wypark.blogbackend.api.common.ApiResponse
import me.wypark.blogbackend.api.dto.ProfileResponse
import me.wypark.blogbackend.domain.profile.BlogProfileService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * [일반 사용자용 프로필 조회 API]
 *
 * 블로그 방문자들에게 운영자(Owner)의 정보를 제공하는 Public 컨트롤러입니다.
 * 수정 권한이 필요한 관리자 영역(AdminProfileController)과 분리하여,
 * 불필요한 인증 로직 없이 누구나 빠르게 조회할 수 있도록 설계된 읽기 전용(Read-Only) 엔드포인트입니다.
 */
@RestController
@RequestMapping("/api/profile")
class ProfileController(
    private val blogProfileService: BlogProfileService
) {

    /**
     * 블로그 운영자의 프로필 정보를 조회합니다.
     *
     * 단일 사용자 블로그(Single User Blog) 특성상 별도의 사용자 ID 파라미터 없이
     * 시스템에 설정된 유일한 프로필 데이터를 반환합니다.
     * (주로 메인 화면의 사이드바나 About 페이지 렌더링에 사용됩니다.)
     */
    @GetMapping
    fun getProfile(): ResponseEntity<ApiResponse<ProfileResponse>> {
        return ResponseEntity.ok(ApiResponse.success(blogProfileService.getProfile()))
    }
}