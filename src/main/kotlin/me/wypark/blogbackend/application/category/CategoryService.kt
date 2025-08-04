package me.wypark.blogbackend.application.category

import me.wypark.blogbackend.application.common.BusinessException
import me.wypark.blogbackend.domain.category.Category
import me.wypark.blogbackend.domain.category.CategoryRepository
import me.wypark.blogbackend.domain.post.PostRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CategoryService(
    private val categoryRepository: CategoryRepository,
    private val postRepository: PostRepository
) {

    fun getCategoryTree(): List<CategoryResponse> {
        return categoryRepository.findAllRoots().map(CategoryResponse::from)
    }

    @Transactional
    fun createCategory(request: CategoryCreateRequest): Long {
        validateAvailableName(request.name)
        val parent = request.parentId?.let { findCategory(it, "부모 카테고리가 존재하지 않습니다.") }
        val category = Category(name = request.name, parent = parent)
        parent?.addChild(category)

        return requireNotNull(categoryRepository.save(category).id) { "Saved category must have an id" }
    }

