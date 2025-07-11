package me.wypark.blogbackend.domain.comment

import me.wypark.blogbackend.domain.post.Post
import org.springframework.data.jpa.repository.JpaRepository

interface CommentRepository : JpaRepository<Comment, Long> {

    fun findAllByPostAndParentIsNullOrderByCreatedAtAsc(post: Post): List<Comment>

    fun deleteAllByPost(post: Post)
}