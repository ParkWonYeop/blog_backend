package me.wypark.blogbackend.domain.profile

import me.wypark.blogbackend.api.dto.ProfileResponse
import me.wypark.blogbackend.api.dto.ProfileUpdateRequest
import me.wypark.blogbackend.domain.image.ImageService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * [프로필 비즈니스 로직]
 *
 * 블로그 운영자의 정보 관리 및 관련 리소스(이미지) 처리를 담당합니다.
 *
 * [단일 리소스 정책]
 * 이 블로그 시스템은 단일 운영자(Single User)를 가정하므로,
 * 프로필 데이터는 테이블 내에 항상 1개의 레코드(Singleton)만 존재하도록 관리됩니다.
 */
@Service
@Transactional(readOnly = true)
class BlogProfileService(
    private val blogProfileRepository: BlogProfileRepository,
    private val imageService: ImageService
) {

    /**
     * 현재 설정된 프로필 정보를 조회합니다.
     *
     * [초기화 전략: Get-Or-Create]
     * 앱 초기 구동 시 프로필 데이터가 없을 경우(Cold Start),
     * 사용자에게 빈 화면이나 에러를 보여주는 대신 기본값(Default)으로 레코드를 생성하여 반환합니다.
     * 이를 통해 별도의 초기화 스크립트 없이도 즉시 서비스를 사용할 수 있습니다.
     */
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

    /**
     * 프로필 정보를 수정합니다.
     *
     * [리소스 최적화: Image Garbage Collection]
     * 프로필 이미지가 변경되거나 삭제될 경우, 더 이상 사용되지 않는 기존 이미지 파일(Dangling File)을
     * 스토리지(S3)에서 즉시 삭제하여 스토리지 비용 낭비를 방지합니다.
     */
    @Transactional
    fun updateProfile(request: ProfileUpdateRequest) {
        // 데이터가 없으면 생성(Upsert)
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

        // [이미지 변경 감지]
        // 요청된 이미지 URL이 기존과 다를 경우 (교체 또는 삭제)
        if (profile.imageUrl != request.imageUrl) {
            // 기존 이미지가 존재했다면 정리 대상이므로 삭제 처리
            if (!profile.imageUrl.isNullOrBlank()) {
                val oldFileName = profile.imageUrl!!.substringAfterLast("/")
                imageService.deleteImage(oldFileName)
            }
        }

        // 엔티티 상태 업데이트 (Dirty Checking에 의해 트랜잭션 종료 시 반영)
        profile.update(
            name = request.name,
            bio = request.bio,
            imageUrl = request.imageUrl,
            githubUrl = request.githubUrl,
            email = request.email
        )
    }
}