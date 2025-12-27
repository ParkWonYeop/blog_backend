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

    @GetMapping
    fun getPosts(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) category: String?, // 👈 프론트는 'category'로 보냄
        @RequestParam(required = false) tag: String?,
        @PageableDefault(size = 10, sort = ["createdAt"], direction = Sort.Direction.DESC) pageable: Pageable
    ): ResponseEntity<ApiResponse<Page<PostSummaryResponse>>> {

        // 검색 조건이 하나라도 있으면 searchPosts 호출 (검색 + 카테고리 필터링)
        return if (keyword != null || category != null || tag != null) {
            val posts = postService.searchPosts(keyword, category, tag, pageable)
            ResponseEntity.ok(ApiResponse.success(posts))
        } else {
            // 조건이 없으면 전체 목록 조회
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