package me.wypark.blogbackend.domain.comment

import me.wypark.blogbackend.domain.post.Post
import org.springframework.data.jpa.repository.JpaRepository

interface CommentRepository : JpaRepository<Comment, Long> {

    // 특정 게시글의 모든 댓글 조회 (최상위 부모 댓글 기준 + 작성순)
    // 자식 댓글은 Entity의 children 필드를 통해 가져오거나, BatchSize로 최적화합니다.
    fun findAllByPostAndParentIsNullOrderByCreatedAtAsc(post: Post): List<Comment>

    // 게시글 삭제 시 관련 댓글 전체 삭제용
    fun deleteAllByPost(post: Post)
}