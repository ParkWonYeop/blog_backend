package me.wypark.blogbackend.domain.post.service

import java.time.LocalDate

interface PostViewCounter {
    fun increment(postId: Long, date: LocalDate)
}

interface PostViewGuard {
    fun shouldCount(postId: Long, date: LocalDate, viewerId: String): Boolean
}
