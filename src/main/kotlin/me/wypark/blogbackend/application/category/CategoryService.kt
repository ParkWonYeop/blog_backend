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

    @Transactional
    fun updateCategory(id: Long, request: CategoryUpdateRequest) {
        val category = findCategory(id, "존재하지 않는 카테고리입니다.")

        if (category.name != request.name) {
            validateAvailableName(request.name)
            category.updateName(request.name)
        }

        if (category.parent?.id != request.parentId) {
            val newParent = request.parentId?.let {
                findCategory(it, "이동하려는 부모 카테고리가 존재하지 않습니다.")
            }
            if (newParent != null) validateHierarchy(category, newParent)
            category.changeParent(newParent)
        }
    }

    @Transactional
    fun deleteCategory(id: Long) {
        val category = findCategory(id, "존재하지 않는 카테고리입니다.")
        val categories = buildList { collectCategoryTree(category, this) }

        postRepository.bulkUpdateCategoryToNull(categories)
        categoryRepository.delete(category)
    }

    private fun validateAvailableName(name: String) {
        if (name.equals(UNCATEGORIZED, ignoreCase = true)) {
            throw BusinessException("'uncategorized'는 시스템 예약어이므로 사용할 수 없습니다.")
        }
