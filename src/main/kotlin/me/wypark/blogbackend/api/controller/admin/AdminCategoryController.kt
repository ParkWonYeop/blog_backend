package me.wypark.blogbackend.api.controller.admin

import me.wypark.blogbackend.api.common.ApiResponse
import me.wypark.blogbackend.api.dto.CategoryCreateRequest
import me.wypark.blogbackend.api.dto.CategoryUpdateRequest
import me.wypark.blogbackend.domain.category.CategoryService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/categories")
class AdminCategoryController(
    private val categoryService: CategoryService
) {

    @PostMapping
    fun createCategory(@RequestBody request: CategoryCreateRequest): ResponseEntity<ApiResponse<Long>> {
        val id = categoryService.createCategory(request)
        return ResponseEntity.ok(ApiResponse.success(id, "카테고리가 생성되었습니다."))
    }

    // 👈 [추가] 카테고리 수정 (이름, 위치 이동)
    @PutMapping("/{id}")
    fun updateCategory(
        @PathVariable id: Long,
        @RequestBody request: CategoryUpdateRequest
    ): ResponseEntity<ApiResponse<Nothing>> {
        categoryService.updateCategory(id, request)
        return ResponseEntity.ok(ApiResponse.success(message = "카테고리가 수정되었습니다."))
    }

    @DeleteMapping("/{id}")
    fun deleteCategory(@PathVariable id: Long): ResponseEntity<ApiResponse<Nothing>> {
        categoryService.deleteCategory(id)
        return ResponseEntity.ok(ApiResponse.success(message = "카테고리가 삭제되었습니다."))
    }
}