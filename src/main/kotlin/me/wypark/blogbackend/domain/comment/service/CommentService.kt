package me.wypark.blogbackend.domain.comment.service
import me.wypark.blogbackend.domain.comment.dto.AdminCommentResponse
import me.wypark.blogbackend.domain.comment.dto.CommentResponse
import me.wypark.blogbackend.domain.comment.dto.CommentSaveRequest

import me.wypark.blogbackend.global.common.BusinessException
import me.wypark.blogbackend.domain.comment.entity.Comment
import me.wypark.blogbackend.domain.comment.repository.CommentRepository
import me.wypark.blogbackend.domain.post.entity.Post
import me.wypark.blogbackend.domain.post.repository.PostRepository
import me.wypark.blogbackend.domain.user.repository.MemberRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
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
        val comments = commentRepository.findAllByPost(
            post,
            PageRequest.of(0, MAX_RESPONSE_COMMENTS, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).asReversed()
        return CommentResponse.fromFlat(comments)
    }

    @Transactional
    fun createComment(request: CommentSaveRequest, userEmail: String?): Long {
        val post = postRepository.findBySlug(request.postSlug)
            ?: throw BusinessException("존재하지 않는 게시글입니다.")
        val parent = request.parentId?.let {
            commentRepository.findByIdOrNull(it)
                ?: throw BusinessException("부모 댓글이 존재하지 않습니다.")
        }
        validateParent(parent, post)

        val comment = if (userEmail == null) {
            createGuestComment(request, post, parent)
        } else {
            val member = memberRepository.findByEmail(userEmail)
                ?: throw BusinessException("회원 정보를 찾을 수 없습니다.")
            Comment(content = request.content, post = post, parent = parent, member = member)
        }

        parent?.addReply(comment)
        return requireNotNull(commentRepository.save(comment).id) { "Saved comment must have an id" }
    }

    @Transactional
    fun deleteComment(commentId: Long, userEmail: String?, guestPassword: String?) {
        val comment = findComment(commentId)
        verifyDeletePermission(comment, userEmail, guestPassword)
        commentRepository.delete(comment)
    }

    @Transactional
    fun deleteCommentByAdmin(commentId: Long) {
        commentRepository.delete(findComment(commentId))
    }

    fun getAllComments(pageable: Pageable): Page<AdminCommentResponse> {
        return commentRepository.findAll(pageable).map(AdminCommentResponse::from)
    }

    private fun createGuestComment(
        request: CommentSaveRequest,
        post: Post,
        parent: Comment?
    ): Comment {
        val nickname = request.guestNickname?.takeUnless(String::isBlank)
            ?: throw BusinessException("비회원은 닉네임과 비밀번호가 필수입니다.")
        val password = request.guestPassword?.takeUnless(String::isBlank)
            ?: throw BusinessException("비회원은 닉네임과 비밀번호가 필수입니다.")
        if (password.toByteArray(Charsets.UTF_8).size > BCRYPT_MAX_BYTES) {
            throw BusinessException("비밀번호는 UTF-8 기준 72바이트 이하로 입력해주세요.")
        }

        return Comment(
            content = request.content,
            post = post,
            parent = parent,
            guestNickname = nickname,
            guestPassword = passwordEncoder.encode(password)
        )
    }

    private fun verifyDeletePermission(comment: Comment, userEmail: String?, guestPassword: String?) {
        if (userEmail != null) {
            if (comment.member?.email != userEmail) {
                throw BusinessException("본인의 댓글만 삭제할 수 있습니다.")
            }
            return
        }

        val encodedPassword = comment.guestPassword
        if (encodedPassword == null || guestPassword == null || guestPassword.toByteArray(Charsets.UTF_8).size > BCRYPT_MAX_BYTES ||
            !passwordEncoder.matches(guestPassword, encodedPassword)
        ) {
            throw BusinessException("비밀번호가 일치하지 않습니다.")
        }
    }

    private fun findComment(id: Long): Comment {
        return commentRepository.findByIdOrNull(id)
            ?: throw BusinessException("존재하지 않는 댓글입니다.")
    }

    private fun validateParent(parent: Comment?, post: Post) {
        if (parent == null) return
        if (parent.post.id != post.id) {
            throw BusinessException("다른 게시글의 댓글에는 답글을 작성할 수 없습니다.")
        }

        var depth = 1
        var ancestor = parent.parent
        while (ancestor != null) {
            depth += 1
            if (depth > MAX_REPLY_DEPTH) {
                throw BusinessException("답글은 ${MAX_REPLY_DEPTH}단계까지만 작성할 수 있습니다.")
            }
            ancestor = ancestor.parent
        }
    }

    companion object {
        private const val MAX_RESPONSE_COMMENTS = 500
        private const val MAX_REPLY_DEPTH = 5
        private const val BCRYPT_MAX_BYTES = 72
    }
}
