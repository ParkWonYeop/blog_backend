package me.wypark.blogbackend.domain.comment

import jakarta.persistence.*
import me.wypark.blogbackend.domain.common.BaseTimeEntity
import me.wypark.blogbackend.domain.post.Post
import me.wypark.blogbackend.domain.user.Member

/**
 * [댓글 엔티티]
 *
 * 게시글에 대한 사용자 반응(Interaction)을 저장하는 도메인 모델입니다.
 *
 * [핵심 설계 전략]
 * 1. 계층형 구조(Hierarchy): 대댓글 기능을 지원하기 위해 자기 자신을 참조(Self-Referencing)하는 구조를 가집니다.
 * 2. 하이브리드 인증 지원: 참여율을 높이기 위해 회원(Member)뿐만 아니라 비회원(Guest)의 작성도 허용하며,
 * 이에 따라 작성자 정보를 조건부로 저장하는 유연한 스키마를 채택했습니다.
 */
@Entity
class Comment(
    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    val post: Post,

    /**
     * [계층형 구조 - 부모 댓글]
     * 최상위 댓글일 경우 null이며, 대댓글(Reply)일 경우 상위 댓글을 참조합니다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    var parent: Comment? = null,

    /**
     * [계층형 구조 - 자식 댓글]
     *
     * [삭제 정책: Cascade & OrphanRemoval]
     * 부모 댓글이 삭제되면 그에 딸린 대댓글들도 논리적으로 존재 가치를 잃게 되므로,
     * 영속성 전이(Cascade)를 통해 DB에서 함께 삭제되도록 설정하여 데이터 정합성을 유지합니다.
     */
    @OneToMany(mappedBy = "parent", cascade = [CascadeType.ALL], orphanRemoval = true)
    val children: MutableList<Comment> = mutableListOf(),

    // =================================================================================
    // [작성자 정보 관리 전략 (Hybrid)]
    // 회원은 Member 연관관계를 사용하고, 비회원은 별도의 컬럼(guest_*)을 사용합니다.
    // =================================================================================

    // 1. 회원일 경우 (FK)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    val member: Member? = null,

    // 2. 비회원일 경우 (임시 식별 정보)
    @Column
    var guestNickname: String? = null,

    /**
     * 비회원용 수정/삭제 비밀번호
     * Note: 보안을 위해 실제 운영 환경에서는 평문 저장이 아닌 단방향 암호화(Hash) 후 저장해야 합니다.
     */
    @Column
    var guestPassword: String? = null

) : BaseTimeEntity() {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    /**
     * 뷰 렌더링을 위한 작성자 이름 반환 로직입니다.
     * 회원 여부에 따라 닉네임 소스(Source)가 달라지므로, 이를 캡슐화하여 클라이언트에 일관된 값을 제공합니다.
     */
    fun getAuthorName(): String {
        return member?.nickname ?: guestNickname ?: "알 수 없음"
    }

    /**
     * 비회원 댓글 삭제 요청 시 권한 검증을 수행합니다.
     */
    fun matchGuestPassword(password: String): Boolean {
        return this.guestPassword == password
    }
}