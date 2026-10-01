package me.wypark.blogbackend.domain.post.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import me.wypark.blogbackend.domain.post.entity.Post
import me.wypark.blogbackend.domain.post.dto.PostSummary
import java.time.LocalDateTime

data class PostNeighborResponse(
    val slug: String,
    val title: String
) {
    companion object {
        fun from(post: Post) = PostNeighborResponse(post.slug, post.title)
    }
}

data class PostResponse(
    val id: Long,
    val title: String,
    val content: String,
    val slug: String,
    val categoryName: String?,
    val viewCount: Long,
    val createdAt: LocalDateTime,
    val prevPost: PostNeighborResponse?,
    val nextPost: PostNeighborResponse?
) {
    companion object {
        fun from(post: Post, previous: Post? = null, next: Post? = null): PostResponse {
            return PostResponse(
                id = requireNotNull(post.id) { "Persisted post must have an id" },
                title = post.title,
                content = post.content,
                slug = post.slug,
                categoryName = post.category?.name,
                viewCount = post.viewCount,
                createdAt = post.createdAt,
                prevPost = previous?.let(PostNeighborResponse::from),
                nextPost = next?.let(PostNeighborResponse::from)
            )
        }
    }
}

data class PostSummaryResponse(
    val id: Long,
    val title: String,
    val slug: String,
    val categoryName: String?,
    val viewCount: Long,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val content: String?
) {
    companion object {
        fun from(post: Post): PostSummaryResponse {
            return PostSummaryResponse(
                id = requireNotNull(post.id) { "Persisted post must have an id" },
                title = post.title,
                slug = post.slug,
                categoryName = post.category?.name,
                viewCount = post.viewCount,
                createdAt = post.createdAt,
                updatedAt = post.updatedAt,
                content = excerpt(post.content)
            )
        }

        fun from(summary: PostSummary): PostSummaryResponse {
            return PostSummaryResponse(
                id = summary.id,
                title = summary.title,
                slug = summary.slug,
                categoryName = summary.categoryName,
                viewCount = summary.viewCount,
                createdAt = summary.createdAt,
                updatedAt = summary.updatedAt,
                content = excerpt(summary.content)
            )
        }

        /** 목록 API는 본문 전체 대신 마크다운을 걷어낸 앞부분만 내려보낸다. */
        private fun excerpt(content: String?, maxLength: Int = 300): String? {
            if (content == null) return null
            val plain = content
                .replace(Regex("""```[\s\S]*?```"""), " ")
                .replace(Regex("""!\[(.*?)]\(.*?\)"""), "$1")
                .replace(Regex("""\[(.*?)]\(.*?\)"""), "$1")
                .replace(Regex("[#*`_~>]"), "")
                .replace(Regex("""\s+"""), " ")
                .trim()
            return plain.take(maxLength)
        }
    }
}

data class PostSaveRequest(
    @field:NotBlank(message = "제목은 필수입니다.")
    @field:Size(max = 200, message = "제목은 200자 이하로 입력해주세요.")
    val title: String,

    @field:NotBlank(message = "본문은 필수입니다.")
    @field:Size(max = 1_000_000, message = "본문은 1,000,000자 이하로 입력해주세요.")
    val content: String,

    @field:Size(max = 200, message = "슬러그는 200자 이하로 입력해주세요.")
    val slug: String? = null,

    val categoryId: Long? = null,

    @field:Size(max = 50, message = "태그는 50개 이하로 입력해주세요.")
    val tags: List<@NotBlank(message = "빈 태그는 사용할 수 없습니다.") @Size(max = 50, message = "태그는 50자 이하로 입력해주세요.") String> = emptyList()
)
