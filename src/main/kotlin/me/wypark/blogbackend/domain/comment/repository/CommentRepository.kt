package me.wypark.blogbackend.domain.comment.repository
import me.wypark.blogbackend.domain.comment.entity.Comment

import me.wypark.blogbackend.domain.post.entity.Post
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface CommentRepository : JpaRepository<Comment, Long> {

    fun findAllByPost(post: Post, pageable: Pageable): List<Comment>

    fun deleteAllByPost(post: Post)
}
