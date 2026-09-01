package me.wypark.blogbackend.domain.profile.controller

import me.wypark.blogbackend.global.common.ApiResponse
import me.wypark.blogbackend.domain.profile.service.BlogProfileService
import me.wypark.blogbackend.domain.profile.dto.ProfileResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/profile")
class ProfileController(
    private val blogProfileService: BlogProfileService
) {

    @GetMapping
    fun getProfile(): ResponseEntity<ApiResponse<ProfileResponse>> {
        return ResponseEntity.ok(ApiResponse.success(blogProfileService.getProfile()))
    }
}
