package me.wypark.blogbackend.api.dto

import me.wypark.blogbackend.domain.comment.Comment
import java.time.LocalDateTime

/**
 * [댓글 응답 DTO - Hierarchical]
 *
 * 게시글 상세 화면에서 댓글 목록을 렌더링하기 위한 데이터 객체입니다.
 * 대댓글(Nested Comment)을 포함하는 재귀적 구조를 가지며, 프론트엔드에서의 추가 가공 없이
 * 즉시 트리 형태로 렌더링할 수 있도록 설계되었습니다.
 */
data class CommentResponse(
    val id: Long,
    val content: String,
    val author: String,

    // UI에서 게시글 작성자의 댓글을 강조(Highlight)하기 위한 플래그
    val isPostAuthor: Boolean,

    // 회원일 경우 프로필 링크 연결 등을 위해 ID 제공 (비회원은 null)
    val memberId: Long?,

    val createdAt: LocalDateTime,

    // 자식 댓글 리스트 (Recursive)
    val children: List<CommentResponse>
) {
    companion object {
        fun from(comment: Comment): CommentResponse {
            // 게시글 작성자 본인이 쓴 댓글인지 확인 (비회원은 member가 null이므로 항상 false)
            val isAuthor = comment.member?.id == comment.post.member.id

            return CommentResponse(
                id = comment.id!!,
                content = comment.content,
                author = comment.getAuthorName(),
                isPostAuthor = isAuthor,
                memberId = comment.member?.id,
                createdAt = comment.createdAt,
                children = comment.children.map { from(it) } // 재귀 호출로 트리 구성
            )
        }
    }
}

/**
 * [댓글 작성 요청 DTO]
 *
 * 회원과 비회원(Guest) 모두가 사용하는 통합 요청 객체입니다.
 *
 * [검증 로직]
 * - 회원: Security Context에서 유저 정보를 가져오므로 guest 필드는 무시됩니다.
 * - 비회원: guestNickname과 guestPassword가 필수값으로 요구됩니다.
 */
data class CommentSaveRequest(
    val postSlug: String,
    val content: String,
    val parentId: Long? = null, // 대댓글(Reply)일 경우 상위 댓글 ID

    // --- 비회원 전용 필드 (Anonymous User) ---
    val guestNickname: String? = null,
    val guestPassword: String? = null // 수정/삭제 권한 인증용 비밀번호 (DB 저장 시 암호화됨)
)

/**
 * [댓글 삭제 요청 DTO]
 *
 * 비회원이 본인의 댓글을 삭제할 때 비밀번호 검증을 위해 사용됩니다.
 * 회원의 경우 JWT 토큰으로 본인 확인이 가능하므로 이 DTO의 필드는 사용되지 않습니다.
 */
data class CommentDeleteRequest(
    val guestPassword: String? = null
)