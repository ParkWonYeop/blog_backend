package me.wypark.blogbackend.application.post

import me.wypark.blogbackend.domain.post.Post
import me.wypark.blogbackend.domain.post.PostSummary
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
                content = post.content
            )
        }
