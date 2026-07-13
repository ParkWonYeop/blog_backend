package me.wypark.blogbackend.api.controller.admin

import me.wypark.blogbackend.api.common.ApiResponse
import me.wypark.blogbackend.application.comment.AdminCommentResponse
import me.wypark.blogbackend.application.comment.CommentService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/comments")
class AdminCommentController(
    private val commentService: CommentService
) {

    @DeleteMapping("/{id}")
    fun deleteComment(@PathVariable id: Long): ResponseEntity<ApiResponse<Nothing>> {
        commentService.deleteCommentByAdmin(id)
        return ResponseEntity.ok(ApiResponse.success(message = "관리자 권한으로 댓글을 삭제했습니다."))
    }

    @GetMapping
    fun getAllComments(
        @PageableDefault(size = 20, sort = ["id"], direction = Sort.Direction.DESC) pageable: Pageable
    ): ResponseEntity<ApiResponse<Page<AdminCommentResponse>>> {
        return ResponseEntity.ok(ApiResponse.success(commentService.getAllComments(pageable)))
    }
}
