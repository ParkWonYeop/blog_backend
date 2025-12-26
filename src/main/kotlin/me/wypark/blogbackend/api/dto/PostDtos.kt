package me.wypark.blogbackend.api.dto

import me.wypark.blogbackend.domain.post.Post
import java.time.LocalDateTime

// [응답] 게시글 상세 정보
data class PostResponse(
    val id: Long,
    val title: String,
    val content: String,
    val slug: String,
    val categoryName: String?,
    val viewCount: Long,
    val createdAt: LocalDateTime
) {
    // Entity -> DTO 변환 편의 메서드
    companion object {
        fun from(post: Post): PostResponse {
            return PostResponse(
                id = post.id!!,
                title = post.title,
                content = post.content,
                slug = post.slug,
                categoryName = post.category?.name,
                viewCount = post.viewCount,
                createdAt = post.createdAt
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
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(post: Post): PostSummaryResponse {
            return PostSummaryResponse(
                id = post.id!!,
                title = post.title,
                slug = post.slug,
                categoryName = post.category?.name,
                viewCount = post.viewCount,
                createdAt = post.createdAt
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