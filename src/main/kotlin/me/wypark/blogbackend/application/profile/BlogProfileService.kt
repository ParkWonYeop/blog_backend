package me.wypark.blogbackend.application.profile

import me.wypark.blogbackend.application.image.ImageService
import me.wypark.blogbackend.domain.profile.BlogProfile
import me.wypark.blogbackend.domain.profile.BlogProfileRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class BlogProfileService(
    private val profileRepository: BlogProfileRepository,
    private val imageService: ImageService
) {

    @Transactional
    fun getProfile(): ProfileResponse {
        return ProfileResponse.from(findOrCreateDefaultProfile())
    }

    @Transactional
    fun updateProfile(request: ProfileUpdateRequest) {
        val profile = profileRepository.findFirstByOrderByIdAsc()
            ?: profileRepository.save(request.toProfile())

        deleteReplacedImage(profile.imageUrl, request.imageUrl)
        profile.update(
            name = request.name,
            bio = request.bio,
            imageUrl = request.imageUrl,
            githubUrl = request.githubUrl,
            email = request.email
        )
    }

    private fun findOrCreateDefaultProfile(): BlogProfile {
        return profileRepository.findFirstByOrderByIdAsc()
            ?: profileRepository.save(
                BlogProfile(
                    name = DEFAULT_NAME,
                    bio = DEFAULT_BIO
                )
            )
    }

    private fun deleteReplacedImage(previousUrl: String?, updatedUrl: String?) {
        if (previousUrl != updatedUrl && !previousUrl.isNullOrBlank()) {
            imageService.deleteImage(previousUrl.substringAfterLast('/'))
        }
    }

    private fun ProfileUpdateRequest.toProfile(): BlogProfile {
        return BlogProfile(
            name = name,
            bio = bio,
            imageUrl = imageUrl,
            githubUrl = githubUrl,
            email = email
        )
    }

    companion object {
        private const val DEFAULT_NAME = "Blog User"
        private const val DEFAULT_BIO = "안녕하세요. 블로그에 오신 것을 환영합니다."
    }
}
