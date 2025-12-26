package me.wypark.blogbackend.api.controller

import me.wypark.blogbackend.api.common.ApiResponse
import me.wypark.blogbackend.api.dto.CategoryResponse
import me.wypark.blogbackend.domain.category.CategoryService
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