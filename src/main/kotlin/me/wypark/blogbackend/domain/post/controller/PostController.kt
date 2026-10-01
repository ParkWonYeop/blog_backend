package me.wypark.blogbackend.domain.post.controller

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.constraints.Size
import me.wypark.blogbackend.global.common.ApiResponse
import me.wypark.blogbackend.domain.post.dto.PostResponse
import me.wypark.blogbackend.domain.post.service.PostService
import me.wypark.blogbackend.domain.post.dto.PostSummaryResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.validation.annotation.Validated

@RestController
@RequestMapping("/api/posts")
@Validated
class PostController(
    private val postService: PostService
) {

    @GetMapping
    fun getPosts(
        @RequestParam(required = false) @Size(max = 100) keyword: String?,
        @RequestParam(required = false) @Size(max = 100) category: String?,
        @RequestParam(required = false) @Size(max = 100) tag: String?,
        @PageableDefault(size = 10, sort = ["createdAt"], direction = Sort.Direction.DESC) pageable: Pageable
    ): ResponseEntity<ApiResponse<Page<PostSummaryResponse>>> {

        return if (keyword != null || category != null || tag != null) {
            val posts = postService.searchPosts(keyword, category, tag, pageable)
            ResponseEntity.ok(ApiResponse.success(posts))
        } else {
            val posts = postService.getPosts(pageable)
            ResponseEntity.ok(ApiResponse.success(posts))
        }
    }

    @GetMapping("/{slug}")
    fun getPost(
        @PathVariable @Size(max = 200) slug: String,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponse<PostResponse>> {
        val viewerId = buildString {
            append(request.remoteAddr ?: "unknown")
            append('|')
            append(request.getHeader("User-Agent")?.take(256).orEmpty())
        }
        val post = postService.getPostBySlug(slug, viewerId)
        return ResponseEntity.ok(ApiResponse.success(post))
    }
}
