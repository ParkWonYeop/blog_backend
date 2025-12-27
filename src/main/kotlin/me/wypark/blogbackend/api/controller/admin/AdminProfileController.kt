package me.wypark.blogbackend.api.controller.admin

import me.wypark.blogbackend.api.common.ApiResponse
import me.wypark.blogbackend.api.dto.ProfileUpdateRequest
import me.wypark.blogbackend.domain.profile.BlogProfileService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/profile")
class AdminProfileController(
    private val blogProfileService: BlogProfileService
) {

    @PutMapping
    fun updateProfile(@RequestBody request: ProfileUpdateRequest): ResponseEntity<ApiResponse<Nothing>> {
        blogProfileService.updateProfile(request)
        return ResponseEntity.ok(ApiResponse.success(message = "프로필이 수정되었습니다."))
    }
}