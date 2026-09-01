package me.wypark.blogbackend.domain.post.dto

import java.time.LocalDateTime

data class PostSummary(
    val id: Long,
    val title: String,
    val slug: String,
    val categoryName: String?,
    val viewCount: Long,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val content: String?
)
