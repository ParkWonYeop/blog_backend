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

@RestController
@RequestMapping("/api/comments")
class CommentController(
    private val commentService: CommentService
) {

    // 댓글 목록 조회
    @GetMapping
    fun getComments(@RequestParam postSlug: String): ResponseEntity<ApiResponse<List<CommentResponse>>> {
        return ResponseEntity.ok(ApiResponse.success(commentService.getComments(postSlug)))
    }

    // 댓글 작성 (회원 or 비회원)
    @PostMapping
    fun createComment(
        @RequestBody request: CommentSaveRequest,
        @AuthenticationPrincipal user: User? // 비회원이면 null이 들어옴
    ): ResponseEntity<ApiResponse<Long>> {
        val email = user?.username // null이면 비회원
        val commentId = commentService.createComment(request, email)
        return ResponseEntity.ok(ApiResponse.success(commentId, "댓글이 등록되었습니다."))
    }

    // 댓글 삭제
    @DeleteMapping("/{id}")
    fun deleteComment(
        @PathVariable id: Long,
        @RequestBody(required = false) request: CommentDeleteRequest?, // 비회원용 비밀번호 바디
        @AuthenticationPrincipal user: User?
    ): ResponseEntity<ApiResponse<Nothing>> {
        val email = user?.username
        val password = request?.guestPassword

        commentService.deleteComment(id, email, password)
        return ResponseEntity.ok(ApiResponse.success(message = "댓글이 삭제되었습니다."))
    }
}