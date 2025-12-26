package me.wypark.blogbackend.api.controller.admin

import jakarta.validation.Valid
import me.wypark.blogbackend.api.common.ApiResponse
import me.wypark.blogbackend.api.dto.PostSaveRequest
import me.wypark.blogbackend.domain.post.PostService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.User
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

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
        // user.username은 email입니다.
        val postId = postService.createPost(request, user.username)
        return ResponseEntity.ok(ApiResponse.success(postId, "게시글이 작성되었습니다."))
    }
}