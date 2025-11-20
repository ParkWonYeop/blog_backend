package me.wypark.blogbackend.api.controller.admin

import me.wypark.blogbackend.api.common.ApiResponse
import me.wypark.blogbackend.application.category.CategoryCreateRequest
import me.wypark.blogbackend.application.category.CategoryService
import me.wypark.blogbackend.application.category.CategoryUpdateRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/categories")
class AdminCategoryController(
    private val categoryService: CategoryService
) {

