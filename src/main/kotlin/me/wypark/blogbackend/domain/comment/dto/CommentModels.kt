package me.wypark.blogbackend.domain.comment.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import me.wypark.blogbackend.domain.comment.entity.Comment
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
        fun from(comment: Comment): CommentResponse = fromTree(comment, depth = 0)

        fun fromFlat(comments: List<Comment>): List<CommentResponse> {
            val nodes = comments.associate { comment ->
                requireNotNull(comment.id) { "Persisted comment must have an id" } to ResponseNode(comment)
            }
            val roots = mutableListOf<ResponseNode>()
            comments.forEach { comment ->
                val node = nodes.getValue(requireNotNull(comment.id))
                val parentNode = comment.parent?.id?.let(nodes::get)
                if (parentNode == null) roots += node else parentNode.children += node
            }
            return roots.map { it.toResponse(depth = 0) }
        }

        private fun fromTree(comment: Comment, depth: Int): CommentResponse {
            return CommentResponse(
                id = requireNotNull(comment.id) { "Persisted comment must have an id" },
                content = comment.content,
                author = comment.getAuthorName(),
                isPostAuthor = comment.member?.id == comment.post.member.id,
                memberId = comment.member?.id,
                createdAt = comment.createdAt,
                children = if (depth >= MAX_RESPONSE_DEPTH) {
                    emptyList()
                } else {
                    comment.children.map { fromTree(it, depth + 1) }
                }
            )
        }

        private const val MAX_RESPONSE_DEPTH = 5

        private data class ResponseNode(
            val comment: Comment,
            val children: MutableList<ResponseNode> = mutableListOf()
        ) {
            fun toResponse(depth: Int): CommentResponse {
                return CommentResponse(
                    id = requireNotNull(comment.id),
                    content = comment.content,
                    author = comment.getAuthorName(),
                    isPostAuthor = comment.member?.id == comment.post.member.id,
                    memberId = comment.member?.id,
                    createdAt = comment.createdAt,
                    children = if (depth >= MAX_RESPONSE_DEPTH) {
                        emptyList()
                    } else {
                        children.map { it.toResponse(depth + 1) }
                    }
                )
            }
        }
    }
}

data class CommentSaveRequest(
    @field:NotBlank(message = "게시글 식별자는 필수입니다.")
    @field:Size(max = 200, message = "게시글 식별자가 너무 깁니다.")
    val postSlug: String,

    @field:NotBlank(message = "댓글 내용을 입력해주세요.")
    @field:Size(max = 2_000, message = "댓글은 2,000자 이하로 입력해주세요.")
    val content: String,

    val parentId: Long? = null,

    @field:Size(max = 20, message = "닉네임은 20자 이하로 입력해주세요.")
    val guestNickname: String? = null,

    @field:Size(min = 4, max = 72, message = "비밀번호는 4자 이상 72자 이하로 입력해주세요.")
    val guestPassword: String? = null
)

data class CommentDeleteRequest(
    @field:Size(min = 4, max = 72, message = "비밀번호는 4자 이상 72자 이하로 입력해주세요.")
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
