package me.wypark.blogbackend.application.comment

import me.wypark.blogbackend.application.common.BusinessException
import me.wypark.blogbackend.domain.comment.Comment
import me.wypark.blogbackend.domain.comment.CommentRepository
import me.wypark.blogbackend.domain.post.Post
import me.wypark.blogbackend.domain.post.PostRepository
import me.wypark.blogbackend.domain.user.MemberRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CommentService(
    private val commentRepository: CommentRepository,
    private val postRepository: PostRepository,
    private val memberRepository: MemberRepository,
    private val passwordEncoder: PasswordEncoder
) {

    fun getComments(postSlug: String): List<CommentResponse> {
        val post = postRepository.findBySlug(postSlug)
            ?: throw BusinessException("존재하지 않는 게시글입니다.")
        return commentRepository.findAllByPostAndParentIsNullOrderByCreatedAtAsc(post)
            .map(CommentResponse::from)
    }

    @Transactional
    fun createComment(request: CommentSaveRequest, userEmail: String?): Long {
        val post = postRepository.findBySlug(request.postSlug)
            ?: throw BusinessException("존재하지 않는 게시글입니다.")
        val parent = request.parentId?.let {
            commentRepository.findByIdOrNull(it)
                ?: throw BusinessException("부모 댓글이 존재하지 않습니다.")
        }
