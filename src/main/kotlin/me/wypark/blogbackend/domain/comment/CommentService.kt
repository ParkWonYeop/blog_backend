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

/**
 * [댓글 비즈니스 로직]
 *
 * 게시글에 대한 사용자 반응(Interaction)을 처리하는 서비스입니다.
 *
 * [핵심 아키텍처: Hybrid Authentication]
 * 사용자 참여율을 높이기 위해 로그인한 '회원'뿐만 아니라 '비회원(Guest)'의 활동도 허용합니다.
 * 이에 따라 작성자 식별 및 권한 검증 로직이 이원화되어 처리됩니다.
 */
@Service
@Transactional(readOnly = true)
class CommentService(
    private val commentRepository: CommentRepository,
    private val postRepository: PostRepository,
    private val memberRepository: MemberRepository,
    private val passwordEncoder: PasswordEncoder
) {

    /**
     * 특정 게시글의 댓글 목록을 계층형(Tree) 구조로 조회합니다.
     *
     * [조회 최적화 전략]
     * DB에서 모든 댓글을 가져와 애플리케이션 메모리에서 트리를 구성하는 대신,
     * 최상위(Root) 댓글만 조회하고 자식 댓글(Children)은 JPA의 관계 매핑과 @BatchSize를 통해
     * 필요 시점에 효율적으로 로딩(Lazy Loading)하는 방식을 택했습니다.
     */
    fun getComments(postSlug: String): List<CommentResponse> {
        val post = postRepository.findBySlug(postSlug)
            ?: throw IllegalArgumentException("존재하지 않는 게시글입니다.")

        // Root 댓글 조회 (자식들은 DTO 변환 과정에서 재귀적으로 호출됨)
        val roots = commentRepository.findAllByPostAndParentIsNullOrderByCreatedAtAsc(post)

        return roots.map { CommentResponse.from(it) }
    }

    /**
     * 댓글을 작성합니다. (회원/비회원 통합 처리)
     *
     * 인증 정보(userEmail) 유무에 따라 도메인 로직이 분기됩니다.
     * - 회원: Member 엔티티와 연관관계를 맺어 영구적인 식별을 보장합니다.
     * - 비회원: 닉네임과 비밀번호를 별도 컬럼에 저장하여 최소한의 식별 및 제어 권한을 부여합니다.
     */
    @Transactional
    fun createComment(request: CommentSaveRequest, userEmail: String?): Long {
        // 1. 게시글 존재 확인
        val post = postRepository.findBySlug(request.postSlug)
            ?: throw IllegalArgumentException("존재하지 않는 게시글입니다.")

        // 2. 부모 댓글 조회 (대댓글인 경우 검증)
        val parent = request.parentId?.let {
            commentRepository.findByIdOrNull(it)
                ?: throw IllegalArgumentException("부모 댓글이 존재하지 않습니다.")
        }

        // 3. 작성자 유형별 엔티티 생성 (Factory Logic)
        val comment = if (userEmail != null) {
            // Case A: 회원 작성
            val member = memberRepository.findByEmail(userEmail)
                ?: throw IllegalArgumentException("회원 정보를 찾을 수 없습니다.")

            Comment(
                content = request.content,
                post = post,
                parent = parent,
                member = member
            )
        } else {
            // Case B: 비회원 작성 (익명성 보장하되, 제어권 확보를 위해 비밀번호 필수)
            if (request.guestNickname.isNullOrBlank() || request.guestPassword.isNullOrBlank()) {
                throw IllegalArgumentException("비회원은 닉네임과 비밀번호가 필수입니다.")
            }

            Comment(
                content = request.content,
                post = post,
                parent = parent,
                guestNickname = request.guestNickname,
                guestPassword = passwordEncoder.encode(request.guestPassword) // 보안상 단방향 암호화 저장
            )
        }

        // 4. 연관관계 편의 메서드 (객체 그래프 정합성 유지)
        parent?.children?.add(comment)

        return commentRepository.save(comment).id!!
    }

    /**
     * 댓글을 삭제합니다.
     *
     * [권한 검증 전략: Ownership Verification]
     * 삭제 요청자가 실제 댓글 작성자인지 확인하는 로직입니다.
     * 회원이라면 로그인 세션 정보를, 비회원이라면 작성 시 입력한 비밀번호를 검증 수단으로 사용합니다.
     */
    @Transactional
    fun deleteComment(commentId: Long, userEmail: String?, guestPassword: String?) {
        val comment = commentRepository.findByIdOrNull(commentId)
            ?: throw IllegalArgumentException("존재하지 않는 댓글입니다.")

        // 권한 검증 분기
        if (userEmail != null) {
            // Case A: 회원 (이메일 불일치 시 예외)
            if (comment.member?.email != userEmail) {
                throw IllegalArgumentException("본인의 댓글만 삭제할 수 있습니다.")
            }
        } else {
            // Case B: 비회원 (비밀번호 검증)
            // DB에 저장된 해시값과 입력된 평문 비밀번호를 대조
            if (comment.guestPassword == null || guestPassword == null ||
                !passwordEncoder.matches(guestPassword, comment.guestPassword)) {
                throw IllegalArgumentException("비밀번호가 일치하지 않습니다.")
            }
        }

        // 검증 통과 시 삭제 수행
        commentRepository.delete(comment)
    }

    /**
     * [관리자 전용] 댓글 강제 삭제
     *
     * 악성 댓글이나 스팸 처리를 위해, 작성자 확인 절차(Ownership Check)를 건너뛰고
     * 관리자 권한으로 즉시 데이터를 제거합니다.
     */
    @Transactional
    fun deleteCommentByAdmin(commentId: Long) {
        val comment = commentRepository.findByIdOrNull(commentId)
            ?: throw IllegalArgumentException("존재하지 않는 댓글입니다.")

        commentRepository.delete(comment)
    }

    /**
     * 관리자 대시보드용 전체 댓글 조회
     * 계층 구조와 무관하게 시간순으로 페이징하여 모니터링 편의성을 제공합니다.
     */
    fun getAllComments(pageable: Pageable): Page<AdminCommentResponse> {
        return commentRepository.findAll(pageable)
            .map { AdminCommentResponse.from(it) }
    }
}