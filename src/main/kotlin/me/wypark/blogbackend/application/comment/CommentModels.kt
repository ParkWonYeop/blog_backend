package me.wypark.blogbackend.application.comment

import me.wypark.blogbackend.domain.comment.Comment
import java.time.LocalDateTime

data class CommentResponse(
    val id: Long,
    val content: String,
    val author: String,
    val isPostAuthor: Boolean,
    val memberId: Long?,
    val createdAt: LocalDateTime,
    val children: List<CommentResponse>
) {
    companion object {
        fun from(comment: Comment): CommentResponse {
            return CommentResponse(
                id = requireNotNull(comment.id) { "Persisted comment must have an id" },
                content = comment.content,
                author = comment.getAuthorName(),
                isPostAuthor = comment.member?.id == comment.post.member.id,
                memberId = comment.member?.id,
                createdAt = comment.createdAt,
                children = comment.children.map(::from)
            )
        }
    }
}

data class CommentSaveRequest(
    val postSlug: String,
    val content: String,
    val parentId: Long? = null,
    val guestNickname: String? = null,
    val guestPassword: String? = null
)

data class CommentDeleteRequest(
    val guestPassword: String? = null
)

data class AdminCommentResponse(
    val id: Long,
    val content: String,
    val author: String,
    val postTitle: String,
    val postSlug: String,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(comment: Comment): AdminCommentResponse {
            return AdminCommentResponse(
                id = requireNotNull(comment.id) { "Persisted comment must have an id" },
                content = comment.content,
                author = comment.getAuthorName(),
                postTitle = comment.post.title,
                postSlug = comment.post.slug,
                createdAt = comment.createdAt
            )
        }
    }
}
