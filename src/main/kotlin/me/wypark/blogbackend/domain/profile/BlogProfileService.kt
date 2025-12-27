package me.wypark.blogbackend.domain.profile

import me.wypark.blogbackend.api.dto.ProfileResponse
import me.wypark.blogbackend.api.dto.ProfileUpdateRequest
import me.wypark.blogbackend.domain.image.ImageService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class BlogProfileService(
    private val blogProfileRepository: BlogProfileRepository,
    private val imageService: ImageService // 👈 이미지 서비스 주입
) {

    // 프로필 조회 (없으면 기본값 생성 후 반환)
    @Transactional
    fun getProfile(): ProfileResponse {
        val profile = blogProfileRepository.findAll().firstOrNull()
            ?: blogProfileRepository.save(
                BlogProfile(
                    name = "Blog User",
                    bio = "안녕하세요. 블로그에 오신 것을 환영합니다.",
                    imageUrl = null,
                    githubUrl = null,
                    email = null
                )
            )
        return ProfileResponse.from(profile)
    }

    // 프로필 수정
    @Transactional
    fun updateProfile(request: ProfileUpdateRequest) {
        val profile = blogProfileRepository.findAll().firstOrNull()
            ?: blogProfileRepository.save(
                BlogProfile(
                    name = request.name,
                    bio = request.bio,
                    imageUrl = request.imageUrl,
                    githubUrl = request.githubUrl,
                    email = request.email
                )
            )

        // 1. 이미지 변경 감지 로직 추가
        // 요청된 URL이 기존 URL과 다르면 (새 이미지로 교체 or 삭제됨)
        if (profile.imageUrl != request.imageUrl) {
            // 기존에 설정된 이미지가 있었다면 S3에서 삭제
            if (!profile.imageUrl.isNullOrBlank()) {
                val oldFileName = profile.imageUrl!!.substringAfterLast("/")
                imageService.deleteImage(oldFileName)
            }
        }

        // 2. 정보 업데이트
        profile.update(
            name = request.name,
            bio = request.bio,
            imageUrl = request.imageUrl,
            githubUrl = request.githubUrl,
            email = request.email
        )
    }
}