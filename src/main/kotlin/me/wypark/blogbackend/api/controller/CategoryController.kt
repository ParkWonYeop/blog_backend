package me.wypark.blogbackend.api.controller

import me.wypark.blogbackend.api.common.ApiResponse
import me.wypark.blogbackend.api.dto.CategoryResponse
import me.wypark.blogbackend.domain.category.CategoryService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * [카테고리 조회 API]
 *
 * 일반 사용자(Public)에게 노출되는 카테고리 관련 엔드포인트입니다.
 * 블로그의 탐색(Navigation) 기능을 담당하며, 데이터 변경이 없는 읽기 전용(Read-Only) 작업만을 수행합니다.
 */
@RestController
@RequestMapping("/api/categories")
class CategoryController(
    private val categoryService: CategoryService
) {

    /**
     * 카테고리 전체 계층 구조를 조회합니다.
     *
     * 프론트엔드 사이드바나 헤더 메뉴 렌더링을 위해 설계되었으며,
     * 불필요한 네트워크 왕복(Round Trip)을 줄이기 위해 한 번의 요청으로 중첩된(Nested) 트리 형태의 전체 데이터를 반환합니다.
     */
    @GetMapping
    fun getCategoryTree(): ResponseEntity<ApiResponse<List<CategoryResponse>>> {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getCategoryTree()))
    }
}