package me.wypark.blogbackend.api.dto

import me.wypark.blogbackend.domain.profile.BlogProfile

/**
 * [프로필 응답 DTO]
 *
 * 블로그 운영자(Owner)의 공개 정보를 렌더링하기 위한 View Object입니다.
 *
 * [설계 의도]
 * 데이터베이스 엔티티(Entity)를 직접 반환하지 않고 DTO로 변환하여,
 * 내부 구현의 변경이 클라이언트(View)에 영향을 미치지 않도록 결합도(Coupling)를 낮췄습니다.
 */
data class ProfileResponse(
    val name: String,
    val bio: String,
    val imageUrl: String?,
    val githubUrl: String?,
    val email: String?
) {
    companion object {
        // Entity -> DTO 변환 (Static Factory Method)
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

/**
 * [프로필 수정 요청 DTO]
 *
 * 관리자 대시보드에서 블로그 설정(운영자 정보)을 변경하기 위한 요청 객체입니다.
 *
 * [유효성 정책]
 * - 이름(name)과 소개(bio)는 블로그의 정체성을 나타내는 필수 항목입니다.
 * - 프로필 이미지나 소셜 링크 등은 선택적(Optional)으로 입력할 수 있도록 Nullable로 설계되었습니다.
 */
data class ProfileUpdateRequest(
    val name: String,
    val bio: String,
    val imageUrl: String?,
    val githubUrl: String?,
    val email: String?
)