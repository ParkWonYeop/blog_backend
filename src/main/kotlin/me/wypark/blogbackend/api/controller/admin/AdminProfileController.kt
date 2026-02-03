package me.wypark.blogbackend.api.controller.admin

import me.wypark.blogbackend.api.common.ApiResponse
import me.wypark.blogbackend.api.dto.ProfileUpdateRequest
import me.wypark.blogbackend.domain.profile.BlogProfileService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * [관리자용 프로필 설정 API]
 *
 * 블로그 운영자의 소개(Bio), 프로필 사진, 소셜 링크 등을 관리하는 컨트롤러입니다.
 * 개인 블로그 특성상 단일 사용자(Owner)에 대한 정보만 존재하므로,
 * 별도의 ID 파라미터 없이 싱글톤(Singleton) 리소스처럼 관리됩니다.
 */
@RestController
@RequestMapping("/api/admin/profile")
class AdminProfileController(
    private val blogProfileService: BlogProfileService
) {

    /**
     * 블로그 프로필 정보를 수정합니다.
     *
     * 단순 텍스트 정보(이름, 소개) 수정뿐만 아니라,
     * 변경된 프로필 이미지 URL을 반영하고 기존 이미지를 정리하는 로직이 서비스 레이어에 포함되어 있습니다.
     * 초기 데이터가 없을 경우(First Run), 수정 요청 시 기본 프로필이 생성(Upsert)됩니다.
     */
    @PutMapping
    fun updateProfile(@RequestBody request: ProfileUpdateRequest): ResponseEntity<ApiResponse<Nothing>> {
        blogProfileService.updateProfile(request)
        return ResponseEntity.ok(ApiResponse.success(message = "프로필이 수정되었습니다."))
    }
}