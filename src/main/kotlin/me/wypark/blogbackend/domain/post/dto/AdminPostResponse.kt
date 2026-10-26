package me.wypark.blogbackend.domain.post.dto

import me.wypark.blogbackend.domain.post.entity.Post
import java.time.LocalDateTime

data class AdminPostResponse(
    val id: Long,
    val title: String,
    val content: String,
    val slug: String,
    val categoryId: Long?,
    val tags: List<String>,
    val viewCount: Long,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(post: Post) = AdminPostResponse(
            requireNotNull(post.id), post.title, post.content, post.slug,
            post.category?.id, post.tags.map { it.tag.name }, post.viewCount, post.updatedAt
        )
    }
}
