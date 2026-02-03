package me.wypark.blogbackend.api.controller.admin

import me.wypark.blogbackend.api.common.ApiResponse
import me.wypark.blogbackend.api.dto.AdminCommentResponse
import me.wypark.blogbackend.api.dto.CommentResponse
import me.wypark.blogbackend.domain.comment.CommentService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * [관리자용 댓글 관리 API]
 *
 * 블로그 내 모든 댓글 활동을 모니터링하고 중재(Moderation)하는 컨트롤러입니다.
 * 개별 게시글 단위로 조회하는 일반 API와 달리, 시스템 전체의 댓글 흐름을 파악하는 데 초점이 맞춰져 있습니다.
 */
@RestController
@RequestMapping("/api/admin/comments")
class AdminCommentController(
    private val commentService: CommentService
) {

    /**
     * 부적절한 댓글을 강제로 삭제합니다 (Moderation).
     *
     * 일반 사용자의 삭제 요청과 달리 작성자 본인 확인 절차(Ownership Check)를 건너뛰고,
     * 관리자 권한으로 즉시 데이터를 제거합니다.
     */
    @DeleteMapping("/{id}")
    fun deleteComment(@PathVariable id: Long): ResponseEntity<ApiResponse<Nothing>> {
        commentService.deleteCommentByAdmin(id)
        return ResponseEntity.ok(ApiResponse.success(message = "관리자 권한으로 댓글을 삭제했습니다."))
    }

    /**
     * 관리자 대시보드용 전체 댓글 목록을 조회합니다.
     *
     * 특정 게시글에 종속되지 않고, 최근 작성된 순서대로 모든 댓글을 페이징하여 반환합니다.
     * 이를 통해 관리자는 스팸이나 악성 댓글 발생 여부를 한눈에 파악할 수 있습니다.
     */
    @GetMapping
    fun getAllComments(
        @PageableDefault(size = 20, sort = ["id"], direction = Sort.Direction.DESC) pageable: Pageable
    ): ResponseEntity<ApiResponse<Page<AdminCommentResponse>>> {
        return ResponseEntity.ok(ApiResponse.success(commentService.getAllComments(pageable)))
    }
}