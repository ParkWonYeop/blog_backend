package me.wypark.blogbackend.domain.category.controller

import me.wypark.blogbackend.global.common.ApiResponse
import me.wypark.blogbackend.domain.category.dto.CategoryResponse
import me.wypark.blogbackend.domain.category.service.CategoryService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/categories")
class CategoryController(
    private val categoryService: CategoryService
) {

    @GetMapping
    fun getCategoryTree(): ResponseEntity<ApiResponse<List<CategoryResponse>>> {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getCategoryTree()))
    }
}
