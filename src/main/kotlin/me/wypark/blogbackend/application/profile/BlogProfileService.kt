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
