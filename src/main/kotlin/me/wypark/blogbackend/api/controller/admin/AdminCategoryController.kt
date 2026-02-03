package me.wypark.blogbackend.api.controller.admin

import me.wypark.blogbackend.api.common.ApiResponse
import me.wypark.blogbackend.api.dto.CategoryCreateRequest
import me.wypark.blogbackend.api.dto.CategoryUpdateRequest
import me.wypark.blogbackend.domain.category.CategoryService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * [관리자용 카테고리 관리 API]
 *
 * 블로그의 카테고리 계층 구조(Tree Structure)를 조작하는 컨트롤러입니다.
 * 단순한 CRUD 외에도 부모-자식 관계 설정 및 구조 변경(이동) 로직을 포함하고 있습니다.
 *
 * Note:
 * - 데이터 무결성을 위해 모든 변경 작업은 트랜잭션 범위 안에서 유효성 검증(순환 참조 방지 등) 후 수행됩니다.
 */
@RestController
@RequestMapping("/api/admin/categories")
class AdminCategoryController(
    private val categoryService: CategoryService
) {

    /**
     * 신규 카테고리를 생성합니다.
     * parentId가 없을 경우 최상위(Root) 카테고리로 생성되며, 있을 경우 해당 노드의 자식으로 연결됩니다.
     */
    @PostMapping
    fun createCategory(@RequestBody request: CategoryCreateRequest): ResponseEntity<ApiResponse<Long>> {
        val id = categoryService.createCategory(request)
        return ResponseEntity.ok(ApiResponse.success(id, "카테고리가 생성되었습니다."))
    }

    /**
     * 카테고리 정보(이름 및 계층 위치)를 수정합니다.
     *
     * 단순 이름 변경뿐만 아니라, 부모 카테고리를 변경하여 트리 구조 내에서 위치를 이동시키는 기능도 수행합니다.
     * 위치 이동 시 순환 참조(Cycle)가 발생하지 않도록 서비스 레이어에서 검증 로직이 수행됩니다.
     */
    @PutMapping("/{id}")
    fun updateCategory(
        @PathVariable id: Long,
        @RequestBody request: CategoryUpdateRequest
    ): ResponseEntity<ApiResponse<Nothing>> {
        categoryService.updateCategory(id, request)
        return ResponseEntity.ok(ApiResponse.success(message = "카테고리가 수정되었습니다."))
    }

    /**
     * 카테고리를 삭제합니다.
     *
     * [삭제 정책]
     * - 하위 카테고리(Children)는 재귀적으로 함께 삭제됩니다 (Cascade).
     * - 해당 카테고리에 속해있던 게시글(Post)들은 삭제되지 않고 '미분류(Category = NULL)' 상태로 변경되어 보존됩니다.
     */
    @DeleteMapping("/{id}")
    fun deleteCategory(@PathVariable id: Long): ResponseEntity<ApiResponse<Nothing>> {
        categoryService.deleteCategory(id)
        return ResponseEntity.ok(ApiResponse.success(message = "카테고리가 삭제되었습니다."))
    }
}