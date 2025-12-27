package me.wypark.blogbackend.api.dto

import me.wypark.blogbackend.domain.profile.BlogProfile

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
    val name: String,
    val bio: String,
    val imageUrl: String?,
    val githubUrl: String?,
    val email: String?
)