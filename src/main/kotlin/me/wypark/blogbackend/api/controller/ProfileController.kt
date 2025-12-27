package me.wypark.blogbackend.api.controller

import me.wypark.blogbackend.api.common.ApiResponse
import me.wypark.blogbackend.api.dto.ProfileResponse
import me.wypark.blogbackend.domain.profile.BlogProfileService
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