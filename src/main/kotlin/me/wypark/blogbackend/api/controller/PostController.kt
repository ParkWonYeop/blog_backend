package me.wypark.blogbackend.api.controller

import me.wypark.blogbackend.api.common.ApiResponse
import me.wypark.blogbackend.api.dto.PostResponse
import me.wypark.blogbackend.api.dto.PostSummaryResponse
import me.wypark.blogbackend.domain.post.PostService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/posts")
class PostController(
    private val postService: PostService
) {

    // 목록 조회 (기본값: 최신순, 10개씩)
    @GetMapping
    fun getPosts(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) tag: String?, // 👈 파라미터 추가
        @PageableDefault(size = 10, sort = ["id"], direction = Sort.Direction.DESC) pageable: Pageable
    ): ResponseEntity<ApiResponse<Page<PostSummaryResponse>>> {

        val result = postService.searchPosts(keyword, category, tag, pageable)
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    // 상세 조회 (Slug)
    @GetMapping("/{slug}")
    fun getPost(@PathVariable slug: String): ResponseEntity<ApiResponse<PostResponse>> {
        return ResponseEntity.ok(ApiResponse.success(postService.getPostBySlug(slug)))
    }
}