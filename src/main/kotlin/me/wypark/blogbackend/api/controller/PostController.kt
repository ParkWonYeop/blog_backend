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

/**
 * [일반 사용자용 게시글 조회 API]
 *
 * 블로그의 핵심 콘텐츠인 게시글(Post) 데이터를 제공하는 Public 컨트롤러입니다.
 * 방문자의 조회 요청을 처리하며, 검색 엔진 최적화(SEO)를 고려하여
 * 내부 식별자(ID)가 아닌 의미 있는 문자열(Slug) 기반의 URL 설계를 채택했습니다.
 */
@RestController
@RequestMapping("/api/posts")
class PostController(
    private val postService: PostService
) {

    /**
     * 게시글 목록을 조회하거나 조건에 맞춰 검색합니다.
     *
     * [통합 검색 엔드포인트]
     * 단순 목록 조회와 필터링(검색) 로직을 하나의 API로 통합하여 프론트엔드 구현을 단순화했습니다.
     * 필터 조건(keyword, category, tag) 유무에 따라 동적 쿼리(QueryDSL) 또는 기본 페이징 쿼리로 분기 처리됩니다.
     *
     * @param keyword 제목 또는 본문 검색어 (Optional)
     * @param category 카테고리 이름 (Optional, 프론트엔드 파라미터명: category)
     * @param tag 태그 이름 (Optional)
     */
    @GetMapping
    fun getPosts(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) tag: String?,
        @PageableDefault(size = 10, sort = ["createdAt"], direction = Sort.Direction.DESC) pageable: Pageable
    ): ResponseEntity<ApiResponse<Page<PostSummaryResponse>>> {

        // 필터 조건이 하나라도 존재하면 동적 쿼리(Search) 실행, 없으면 기본 목록 조회(List) 수행
        return if (keyword != null || category != null || tag != null) {
            val posts = postService.searchPosts(keyword, category, tag, pageable)
            ResponseEntity.ok(ApiResponse.success(posts))
        } else {
            val posts = postService.getPosts(pageable)
            ResponseEntity.ok(ApiResponse.success(posts))
        }
    }

    /**
     * 게시글 상세 정보를 조회합니다.
     *
     * URL에 ID(숫자) 대신 제목 기반의 Slug를 사용하여 가독성과 SEO 점수를 높입니다.
     * 상세 조회 성공 시, 서비스 레이어에서 조회수(View Count) 증가 트랜잭션이 함께 수행됩니다.
     */
    @GetMapping("/{slug}")
    fun getPost(@PathVariable slug: String): ResponseEntity<ApiResponse<PostResponse>> {
        val post = postService.getPostBySlug(slug)
        return ResponseEntity.ok(ApiResponse.success(post))
    }
}