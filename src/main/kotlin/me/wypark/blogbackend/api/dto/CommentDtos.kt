package me.wypark.blogbackend.api.dto

import me.wypark.blogbackend.domain.comment.Comment
import java.time.LocalDateTime

// [응답] 댓글 (계층형 구조)
data class CommentResponse(
    val id: Long,
    val content: String,
    val author: String,
    val isPostAuthor: Boolean, // 👈 [추가] 게시글 작성자 여부
    val createdAt: LocalDateTime,
    val children: List<CommentResponse>
) {
    companion object {
        fun from(comment: Comment): CommentResponse {
            // 게시글 작성자 ID와 댓글 작성자(회원) ID가 같은지 비교
            // comment.member는 비회원일 경우 null이므로 안전하게 처리됨
            val isAuthor = comment.member?.id == comment.post.member.id

            return CommentResponse(
                id = comment.id!!,
                content = comment.content,
                author = comment.getAuthorName(),
                isPostAuthor = isAuthor, // 👈 계산된 값 주입
                createdAt = comment.createdAt,
                children = comment.children.map { from(it) }
            )
        }
    }
}
// [요청] 댓글 작성
data class CommentSaveRequest(
    val postSlug: String,
    val content: String,
    val parentId: Long? = null, // 대댓글일 경우 부모 ID

    // 비회원 전용 필드 (회원은 null 가능)
    val guestNickname: String? = null,
    val guestPassword: String? = null
)

// [요청] 댓글 삭제 (비회원용 비밀번호 전달)
data class CommentDeleteRequest(
    val guestPassword: String? = null
)