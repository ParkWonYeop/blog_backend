package me.wypark.blogbackend.api.dto

import me.wypark.blogbackend.domain.post.Post
import java.time.LocalDateTime

// [응답] 인접 게시글 정보 (이전글/다음글)
data class PostNeighborResponse(
    val slug: String,
    val title: String
) {
    companion object {
        fun from(post: Post): PostNeighborResponse {
            return PostNeighborResponse(
                slug = post.slug,
                title = post.title
            )
        }
    }
}

// [응답] 게시글 상세 정보
data class PostResponse(
    val id: Long,
    val title: String,
    val content: String,
    val slug: String,
    val categoryName: String?,
    val viewCount: Long,
    val createdAt: LocalDateTime,
    // 👈 [추가] 이전/다음 게시글 정보
    val prevPost: PostNeighborResponse?,
    val nextPost: PostNeighborResponse?
) {
    // Entity -> DTO 변환 편의 메서드
    companion object {
        fun from(post: Post, prevPost: Post? = null, nextPost: Post? = null): PostResponse {
            return PostResponse(
                id = post.id!!,
                title = post.title,
                content = post.content,
                slug = post.slug,
                categoryName = post.category?.name,
                viewCount = post.viewCount,
                createdAt = post.createdAt,
                prevPost = prevPost?.let { PostNeighborResponse.from(it) },
                nextPost = nextPost?.let { PostNeighborResponse.from(it) }
            )
        }
    }
}

// [응답] 게시글 목록용 (본문 제외, 가볍게)
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
                id = post.id!!,
                title = post.title,
                slug = post.slug,
                categoryName = post.category?.name,
                viewCount = post.viewCount,
                createdAt = post.createdAt,
                updatedAt = post.updatedAt,
                content = post.content
            )
        }
    }
}

// [요청] 게시글 작성/수정
data class PostSaveRequest(
    val title: String,
    val content: String, // 마크다운 원문
    val slug: String? = null,
    val categoryId: Long? = null,
    val tags: List<String> = emptyList() // 태그는 나중에 구현
)