package me.wypark.blogbackend.api.controller

import me.wypark.blogbackend.api.common.ApiResponse
import me.wypark.blogbackend.api.dto.CommentDeleteRequest
import me.wypark.blogbackend.api.dto.CommentResponse
import me.wypark.blogbackend.api.dto.CommentSaveRequest
import me.wypark.blogbackend.domain.comment.CommentService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.User
import org.springframework.web.bind.annotation.*

/**
 * [일반 사용자용 댓글 API]
 *
 * 게시글에 대한 사용자 참여(Social Interaction)를 담당하는 컨트롤러입니다.
 * 사용자 경험(UX)을 고려하여, 회원가입 없이도 자유롭게 소통할 수 있도록
 * 회원(Member)과 비회원(Guest)의 접근을 동시에 허용하는 하이브리드 로직을 수행합니다.
 */
@RestController
@RequestMapping("/api/comments")
class CommentController(
    private val commentService: CommentService
) {

    /**
     * 특정 게시글의 전체 댓글 목록을 조회합니다.
     *
     * 단순 리스트가 아닌, 대댓글(Nested Comments) 구조를 유지한 상태로 반환하여
     * 클라이언트가 별도의 재귀 로직 구현 없이 트리 형태로 즉시 렌더링할 수 있도록 지원합니다.
     */
    @GetMapping
    fun getComments(@RequestParam postSlug: String): ResponseEntity<ApiResponse<List<CommentResponse>>> {
        return ResponseEntity.ok(ApiResponse.success(commentService.getComments(postSlug)))
    }

    /**
     * 댓글을 작성합니다 (회원/비회원 공용).
     *
     * 참여 장벽을 낮추기 위해 로그인 여부를 강제하지 않습니다.
     * Security Context의 User 객체가 null일 경우 비회원으로 간주하며,
     * 이 경우 RequestBody에 포함된 닉네임과 비밀번호를 사용하여 임시 신원을 생성합니다.
     */
    @PostMapping
    fun createComment(
        @RequestBody request: CommentSaveRequest,
        @AuthenticationPrincipal user: User? // 비회원 접근 시 null (Optional Principal)
    ): ResponseEntity<ApiResponse<Long>> {
        val email = user?.username // 인증된 사용자라면 email 추출
        val commentId = commentService.createComment(request, email)
        return ResponseEntity.ok(ApiResponse.success(commentId, "댓글이 등록되었습니다."))
    }

    /**
     * 댓글을 삭제합니다.
     *
     * 작성자 유형(회원/비회원)에 따라 검증 전략(Strategy)이 달라집니다.
     * - 회원: 현재 로그인한 사용자의 ID와 댓글 작성자 ID의 일치 여부를 검증
     * - 비회원: 댓글 작성 시 설정한 비밀번호(Guest Password)의 일치 여부를 검증
     */
    @DeleteMapping("/{id}")
    fun deleteComment(
        @PathVariable id: Long,
        @RequestBody(required = false) request: CommentDeleteRequest?, // 비회원일 경우에만 바디가 필요함
        @AuthenticationPrincipal user: User?
    ): ResponseEntity<ApiResponse<Nothing>> {
        val email = user?.username
        val password = request?.guestPassword

        commentService.deleteComment(id, email, password)
        return ResponseEntity.ok(ApiResponse.success(message = "댓글이 삭제되었습니다."))
    }
}