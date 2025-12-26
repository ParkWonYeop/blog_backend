package me.wypark.blogbackend.api.dto

import me.wypark.blogbackend.domain.comment.Comment
import java.time.LocalDateTime

// [응답] 댓글 (계층형 구조)
data class CommentResponse(
    val id: Long,
    val content: String,
    val author: String, // 회원 닉네임 또는 비회원 닉네임
    val createdAt: LocalDateTime,
    val children: List<CommentResponse> // 대댓글 리스트
) {
    companion object {
        fun from(comment: Comment): CommentResponse {
            return CommentResponse(
                id = comment.id!!,
                content = comment.content,
                author = comment.getAuthorName(), // Entity에 만들어둔 편의 메서드 사용
                createdAt = comment.createdAt,
                children = comment.children.map { from(it) } // 재귀적으로 자식 변환
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