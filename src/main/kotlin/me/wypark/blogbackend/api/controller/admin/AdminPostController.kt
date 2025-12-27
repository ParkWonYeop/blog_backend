package me.wypark.blogbackend.api.controller.admin

import jakarta.validation.Valid
import me.wypark.blogbackend.api.common.ApiResponse
import me.wypark.blogbackend.api.dto.PostSaveRequest
import me.wypark.blogbackend.domain.post.PostService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.User
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/posts")
class AdminPostController(
    private val postService: PostService
) {

    @PostMapping
    fun createPost(
        @RequestBody @Valid request: PostSaveRequest,
        @AuthenticationPrincipal user: User
    ): ResponseEntity<ApiResponse<Long>> {
        val postId = postService.createPost(request, user.username)
        return ResponseEntity.ok(ApiResponse.success(postId, "게시글이 작성되었습니다."))
    }

    // 👈 [추가] 게시글 수정 엔드포인트
    @PutMapping("/{id}")
    fun updatePost(
        @PathVariable id: Long,
        @RequestBody @Valid request: PostSaveRequest
    ): ResponseEntity<ApiResponse<Long>> {
        val postId = postService.updatePost(id, request)
        return ResponseEntity.ok(ApiResponse.success(postId, "게시글이 수정되었습니다."))
    }

    @DeleteMapping("/{id}")
    fun deletePost(@PathVariable id: Long): ResponseEntity<ApiResponse<Nothing>> {
        postService.deletePost(id)
        return ResponseEntity.ok(ApiResponse.success(message = "게시글과 포함된 이미지가 삭제되었습니다."))
    }
}