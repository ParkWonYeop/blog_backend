package me.wypark.blogbackend.domain.comment

import jakarta.persistence.*
import me.wypark.blogbackend.domain.common.BaseTimeEntity
import me.wypark.blogbackend.domain.post.Post
import me.wypark.blogbackend.domain.user.Member

@Entity
class Comment(
    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    val post: Post,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    var parent: Comment? = null, // 대댓글용 부모 댓글

    @OneToMany(mappedBy = "parent", cascade = [CascadeType.ALL], orphanRemoval = true)
    val children: MutableList<Comment> = mutableListOf(),

    // --- 1. 회원일 경우 ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    val member: Member? = null,

    // --- 2. 비회원일 경우 ---
    @Column
    var guestNickname: String? = null,

    @Column
    var guestPassword: String? = null // 암호화해서 저장 권장

) : BaseTimeEntity() {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    // 댓글 작성자 이름 가져오기 (회원이면 닉네임, 비회원이면 입력한 이름)
    fun getAuthorName(): String {
        return member?.nickname ?: guestNickname ?: "알 수 없음"
    }

    // 비회원 비밀번호 검증
    fun matchGuestPassword(password: String): Boolean {
        return this.guestPassword == password
    }
}