package me.wypark.blogbackend.api.controller

import me.wypark.blogbackend.api.common.ApiResponse
import me.wypark.blogbackend.application.comment.CommentDeleteRequest
import me.wypark.blogbackend.application.comment.CommentResponse
import me.wypark.blogbackend.application.comment.CommentSaveRequest
import me.wypark.blogbackend.application.comment.CommentService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.User
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/comments")
class CommentController(
    private val commentService: CommentService
) {

    @GetMapping
    fun getComments(@RequestParam postSlug: String): ResponseEntity<ApiResponse<List<CommentResponse>>> {
        return ResponseEntity.ok(ApiResponse.success(commentService.getComments(postSlug)))
    }

    @PostMapping
    fun createComment(
        @RequestBody request: CommentSaveRequest,
        @AuthenticationPrincipal user: User?
    ): ResponseEntity<ApiResponse<Long>> {
        val email = user?.username
        val commentId = commentService.createComment(request, email)
        return ResponseEntity.ok(ApiResponse.success(commentId, "댓글이 등록되었습니다."))
    }

    @DeleteMapping("/{id}")
    fun deleteComment(
        @PathVariable id: Long,
        @RequestBody(required = false) request: CommentDeleteRequest?,
        @AuthenticationPrincipal user: User?
    ): ResponseEntity<ApiResponse<Nothing>> {
        val email = user?.username
        val password = request?.guestPassword

        commentService.deleteComment(id, email, password)
        return ResponseEntity.ok(ApiResponse.success(message = "댓글이 삭제되었습니다."))
    }
}
