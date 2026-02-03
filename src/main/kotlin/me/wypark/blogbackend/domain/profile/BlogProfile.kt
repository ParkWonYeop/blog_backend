package me.wypark.blogbackend.domain.profile

import jakarta.persistence.*
import me.wypark.blogbackend.domain.common.BaseTimeEntity

/**
 * [블로그 프로필 엔티티]
 *
 * 블로그 운영자(Owner)의 공개적인 신원 정보(Identity)를 관리하는 도메인 모델입니다.
 *
 * [설계 의도: 관심사의 분리 (Separation of Concerns)]
 * 인증/인가를 담당하는 Member 엔티티와 의도적으로 분리하여 설계했습니다.
 * - Member: 시스템 접속 및 보안을 위한 계정 정보 (Email, Password, Role) -> 보안 중요, 변경 빈도 낮음
 * - BlogProfile: 방문자에게 보여지는 소개 정보 (Bio, Social Links) -> 공개 데이터, 변경 빈도 높음
 * 이렇게 책임을 분리함으로써, 프로필 정보 수정 로직이 핵심 인증 데이터에 영향을 주지 않도록 격리했습니다.
 */
@Entity
@Table(name = "blog_profile")
class BlogProfile(
    @Column(nullable = false)
    var name: String,

    // 사용자의 긴 자기소개를 수용하기 위해 대용량 텍스트(CLOB) 타입으로 매핑
    @Column(columnDefinition = "TEXT")
    var bio: String,

    // S3/MinIO 등에 업로드된 이미지 리소스의 절대 경로(URL)
    @Column
    var imageUrl: String? = null,

    @Column
    var githubUrl: String? = null,

    @Column
    var email: String? = null
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    /**
     * 프로필 정보를 갱신합니다.
     *
     * 단순 Setter 나열을 지양하고, 의미 있는 비즈니스 메서드(Update)를 정의하여
     * 한 번의 트랜잭션 내에서 관련된 모든 정보가 원자적(Atomic)으로 변경됨을 명시합니다.
     */
    fun update(name: String, bio: String, imageUrl: String?, githubUrl: String?, email: String?) {
        this.name = name
        this.bio = bio
        this.imageUrl = imageUrl
        this.githubUrl = githubUrl
        this.email = email
    }
}