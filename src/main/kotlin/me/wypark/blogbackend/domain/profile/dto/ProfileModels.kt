package me.wypark.blogbackend.domain.profile.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import me.wypark.blogbackend.domain.profile.entity.BlogProfile

data class ProfileResponse(
    val name: String,
    val bio: String,
    val imageUrl: String?,
    val githubUrl: String?,
    val email: String?
) {
    companion object {
        fun from(profile: BlogProfile): ProfileResponse {
            return ProfileResponse(
                name = profile.name,
                bio = profile.bio,
                imageUrl = profile.imageUrl,
                githubUrl = profile.githubUrl,
                email = profile.email
            )
        }
    }
}

data class ProfileUpdateRequest(
    @field:NotBlank(message = "이름은 필수입니다.")
    @field:Size(max = 100, message = "이름은 100자 이하로 입력해주세요.")
    val name: String,

    @field:Size(max = 2_000, message = "소개는 2,000자 이하로 입력해주세요.")
    val bio: String,

    @field:Size(max = 2_048, message = "이미지 URL이 너무 깁니다.")
    @field:Pattern(regexp = "^$|^https?://.+", message = "이미지 URL은 http 또는 https 주소여야 합니다.")
    val imageUrl: String?,

    @field:Size(max = 2_048, message = "GitHub URL이 너무 깁니다.")
    @field:Pattern(regexp = "^$|^https?://.+", message = "GitHub URL은 http 또는 https 주소여야 합니다.")
    val githubUrl: String?,

    @field:Email(message = "올바른 이메일 형식이 아닙니다.")
    @field:Size(max = 254, message = "이메일은 254자 이하로 입력해주세요.")
    val email: String?
)
