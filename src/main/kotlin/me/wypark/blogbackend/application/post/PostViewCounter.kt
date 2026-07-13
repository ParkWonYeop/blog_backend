package me.wypark.blogbackend.application.post

import java.time.LocalDate

interface PostViewCounter {
    fun increment(postId: Long, date: LocalDate)
}
