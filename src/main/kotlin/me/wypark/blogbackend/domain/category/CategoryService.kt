package me.wypark.blogbackend.domain.category

import me.wypark.blogbackend.api.dto.CategoryCreateRequest
import me.wypark.blogbackend.api.dto.CategoryResponse
import me.wypark.blogbackend.api.dto.CategoryUpdateRequest
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

    // 예약어 검증 메서드
    private fun validateReservedName(name: String) {
        if (name.equals("uncategorized", ignoreCase = true)) {
            throw IllegalArgumentException("'uncategorized'는 시스템 예약어이므로 사용할 수 없습니다.")
        }
    }

    fun getCategoryTree(): List<CategoryResponse> {
        val roots = categoryRepository.findAllRoots()
        return roots.map { CategoryResponse.from(it) }
    }

    @Transactional
    fun createCategory(request: CategoryCreateRequest): Long {
        // 1. 예약어 검증
        validateReservedName(request.name)

        // 2. 중복 체크
        if (categoryRepository.existsByName(request.name)) {
            throw IllegalArgumentException("이미 존재하는 카테고리 이름입니다.")
        }

        val parent = request.parentId?.let {
            categoryRepository.findByIdOrNull(it)
                ?: throw IllegalArgumentException("부모 카테고리가 존재하지 않습니다.")
        }

        val category = Category(
            name = request.name,
            parent = parent
        )

        parent?.addChild(category)

        return categoryRepository.save(category).id!!
    }

    @Transactional
    fun updateCategory(id: Long, request: CategoryUpdateRequest) {
        val category = categoryRepository.findByIdOrNull(id)
            ?: throw IllegalArgumentException("존재하지 않는 카테고리입니다.")

        // 1. 이름 변경 (변경 시에만 검증)
        if (request.name != null && category.name != request.name) {
            validateReservedName(request.name) // 예약어 검증

            if (categoryRepository.existsByName(request.name)) {
                throw IllegalArgumentException("이미 존재하는 카테고리 이름입니다.")
            }
            category.updateName(request.name)
        }

        // 2. 부모 변경
        val currentParentId = category.parent?.id
        val newParentId = request.parentId

        if (currentParentId != newParentId) {
            if (newParentId == null) {
                category.changeParent(null)
            } else {
                val newParent = categoryRepository.findByIdOrNull(newParentId)
                    ?: throw IllegalArgumentException("이동하려는 부모 카테고리가 존재하지 않습니다.")

                validateHierarchy(category, newParent)
                category.changeParent(newParent)
            }
        }
    }

    private fun validateHierarchy(target: Category, newParent: Category) {
        if (target.id == newParent.id) {
            throw IllegalArgumentException("자기 자신을 부모로 설정할 수 없습니다.")
        }

        var parent = newParent.parent
        while (parent != null) {
            if (parent.id == target.id) {
                throw IllegalArgumentException("자신의 하위 카테고리 밑으로 이동할 수 없습니다.")
            }
            parent = parent.parent
        }
    }

    @Transactional
    fun deleteCategory(id: Long) {
        val category = categoryRepository.findByIdOrNull(id)
            ?: throw IllegalArgumentException("존재하지 않는 카테고리입니다.")

        val categoriesToDelete = mutableListOf<Category>()
        collectAllCategories(category, categoriesToDelete)

        postRepository.bulkUpdateCategoryToNull(categoriesToDelete)

        categoryRepository.delete(category)
    }

    private fun collectAllCategories(category: Category, list: MutableList<Category>) {
        list.add(category)
        category.children.forEach { collectAllCategories(it, list) }
    }
}