package me.wypark.blogbackend.api.controller

import me.wypark.blogbackend.api.common.ApiResponse
import me.wypark.blogbackend.application.post.PostResponse
import me.wypark.blogbackend.application.post.PostService
import me.wypark.blogbackend.application.post.PostSummaryResponse
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

@RestController
@RequestMapping("/api/posts")
class PostController(
    private val postService: PostService
) {

    @GetMapping
    fun getPosts(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) tag: String?,
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
    fun getPost(@PathVariable slug: String): ResponseEntity<ApiResponse<PostResponse>> {
        val post = postService.getPostBySlug(slug)
        return ResponseEntity.ok(ApiResponse.success(post))
    }
}
