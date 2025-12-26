package me.wypark.blogbackend.domain.comment

import me.wypark.blogbackend.api.dto.AdminCommentResponse
import me.wypark.blogbackend.api.dto.CommentResponse
import me.wypark.blogbackend.api.dto.CommentSaveRequest
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
    private val passwordEncoder: PasswordEncoder // 비밀번호 암호화용
) {

    /**
     * [Public] 특정 게시글의 댓글 목록 조회 (계층형)
     */
    fun getComments(postSlug: String): List<CommentResponse> {
        val post = postRepository.findBySlug(postSlug)
            ?: throw IllegalArgumentException("존재하지 않는 게시글입니다.")

        // 최상위(부모가 null) 댓글만 가져오면, Entity 설정에 의해 자식들은 자동으로 딸려옴
        val roots = commentRepository.findAllByPostAndParentIsNullOrderByCreatedAtAsc(post)

        return roots.map { CommentResponse.from(it) }
    }

    /**
     * [Hybrid] 댓글 작성 (회원/비회원 공용)
     */
    @Transactional
    fun createComment(request: CommentSaveRequest, userEmail: String?): Long {
        // 1. 게시글 조회
        val post = postRepository.findBySlug(request.postSlug)
            ?: throw IllegalArgumentException("존재하지 않는 게시글입니다.")

        // 2. 부모 댓글 조회 (대댓글인 경우)
        val parent = request.parentId?.let {
            commentRepository.findByIdOrNull(it)
                ?: throw IllegalArgumentException("부모 댓글이 존재하지 않습니다.")
        }

        // 3. 회원/비회원 구분 로직
        val comment = if (userEmail != null) {
            // [회원] DB에서 회원 정보 조회 후 연결
            val member = memberRepository.findByEmail(userEmail)
                ?: throw IllegalArgumentException("회원 정보를 찾을 수 없습니다.")

            Comment(
                content = request.content,
                post = post,
                parent = parent,
                member = member // 회원 연결
            )
        } else {
            // [비회원] 닉네임/비밀번호 필수 체크
            if (request.guestNickname.isNullOrBlank() || request.guestPassword.isNullOrBlank()) {
                throw IllegalArgumentException("비회원은 닉네임과 비밀번호가 필수입니다.")
            }

            Comment(
                content = request.content,
                post = post,
                parent = parent,
                guestNickname = request.guestNickname,
                guestPassword = passwordEncoder.encode(request.guestPassword)
            )
        }

        // 4. 부모가 있다면 연결 (양방향 편의)
        parent?.children?.add(comment)

        return commentRepository.save(comment).id!!
    }

    @Transactional
    fun deleteComment(commentId: Long, userEmail: String?, guestPassword: String?) {
        val comment = commentRepository.findByIdOrNull(commentId)
            ?: throw IllegalArgumentException("존재하지 않는 댓글입니다.")

        // 권한 검증
        if (userEmail != null) {
            // [회원] 본인 댓글인지 확인 (이메일 비교)
            if (comment.member?.email != userEmail) {
                throw IllegalArgumentException("본인의 댓글만 삭제할 수 있습니다.")
            }
        } else {
            // [비회원] 비밀번호 일치 확인
            if (comment.guestPassword == null || guestPassword == null ||
                !passwordEncoder.matches(guestPassword, comment.guestPassword)) {
                throw IllegalArgumentException("비밀번호가 일치하지 않습니다.")
            }
        }

        // 삭제 진행
        commentRepository.delete(comment)
    }

    @Transactional
    fun deleteCommentByAdmin(commentId: Long) {
        val comment = commentRepository.findByIdOrNull(commentId)
            ?: throw IllegalArgumentException("존재하지 않는 댓글입니다.")

        commentRepository.delete(comment)
    }

    fun getAllComments(pageable: Pageable): Page<AdminCommentResponse> {
        return commentRepository.findAll(pageable)
            .map { AdminCommentResponse.from(it) }
    }
}