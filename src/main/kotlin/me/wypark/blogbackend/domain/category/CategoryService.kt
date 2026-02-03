package me.wypark.blogbackend.domain.category

import me.wypark.blogbackend.api.dto.CategoryCreateRequest
import me.wypark.blogbackend.api.dto.CategoryResponse
import me.wypark.blogbackend.api.dto.CategoryUpdateRequest
import me.wypark.blogbackend.domain.post.PostRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * [카테고리 비즈니스 로직]
 *
 * 카테고리의 생성, 수정, 삭제 및 계층 구조(Tree) 관리를 담당합니다.
 * 단순한 데이터 조작을 넘어, 순환 참조(Cycle) 방지와 같은 구조적 무결성 검증 로직이 포함되어 있습니다.
 */
@Service
@Transactional(readOnly = true)
class CategoryService(
    private val categoryRepository: CategoryRepository,
    private val postRepository: PostRepository
) {

    /**
     * 시스템 예약어 사용을 방지합니다.
     * '미분류(uncategorized)' 등 시스템 내부 로직에서 특별하게 취급하는 이름은
     * 사용자가 임의로 생성하거나 수정할 수 없도록 제한합니다.
     */
    private fun validateReservedName(name: String) {
        if (name.equals("uncategorized", ignoreCase = true)) {
            throw IllegalArgumentException("'uncategorized'는 시스템 예약어이므로 사용할 수 없습니다.")
        }
    }

    /**
     * 전체 카테고리를 계층형 트리 구조로 반환합니다.
     * Root 노드만 조회하면, 엔티티 내의 연관관계와 Fetch Join을 통해 하위 노드들이 재귀적으로 매핑됩니다.
     */
    fun getCategoryTree(): List<CategoryResponse> {
        val roots = categoryRepository.findAllRoots()
        return roots.map { CategoryResponse.from(it) }
    }

    /**
     * 신규 카테고리를 생성합니다.
     * 데이터 정합성을 위해 이름 중복 검사와 부모 카테고리의 존재 여부를 엄격하게 검증(Strict Validation)합니다.
     */
    @Transactional
    fun createCategory(request: CategoryCreateRequest): Long {
        // 1. 예약어 검증
        validateReservedName(request.name)

        // 2. 중복 체크 (Unique Constraint)
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

    /**
     * 카테고리 정보를 수정합니다. (이름 변경 및 트리 구조 이동)
     *
     * [구조 변경 시 주의사항]
     * 부모 카테고리를 변경하는 경우, 트리 구조가 깨지거나 순환 참조(Cycle)가 발생할 위험이 있습니다.
     * 따라서 이동 전에 `validateHierarchy`를 통해 구조적 유효성을 반드시 확인해야 합니다.
     */
    @Transactional
    fun updateCategory(id: Long, request: CategoryUpdateRequest) {
        val category = categoryRepository.findByIdOrNull(id)
            ?: throw IllegalArgumentException("존재하지 않는 카테고리입니다.")

        // 1. 이름 변경 (실제 변경이 있을 때만 검증 수행)
        if (request.name != null && category.name != request.name) {
            validateReservedName(request.name)

            if (categoryRepository.existsByName(request.name)) {
                throw IllegalArgumentException("이미 존재하는 카테고리 이름입니다.")
            }
            category.updateName(request.name)
        }

        // 2. 부모 변경 (구조 이동)
        val currentParentId = category.parent?.id
        val newParentId = request.parentId

        if (currentParentId != newParentId) {
            if (newParentId == null) {
                // Root로 이동
                category.changeParent(null)
            } else {
                // 다른 하위 노드로 이동
                val newParent = categoryRepository.findByIdOrNull(newParentId)
                    ?: throw IllegalArgumentException("이동하려는 부모 카테고리가 존재하지 않습니다.")

                // 순환 참조 검증
                validateHierarchy(category, newParent)
                category.changeParent(newParent)
            }
        }
    }

    /**
     * [순환 참조 방지 로직]
     *
     * 카테고리 이동 시, 대상(Target)이 자신의 하위 카테고리로 들어가는 것을 방지합니다.
     * 만약 허용할 경우, A -> B -> A 형태의 무한 루프가 발생하여 트리 조회가 불가능해집니다.
     *
     * @param target 이동하려는 카테고리
     * @param newParent 이동할 목적지(새 부모)
     */
    private fun validateHierarchy(target: Category, newParent: Category) {
        // 1. 자기 자신을 부모로 설정하는 경우
        if (target.id == newParent.id) {
            throw IllegalArgumentException("자기 자신을 부모로 설정할 수 없습니다.")
        }

        // 2. 자신의 자손(Descendant)을 부모로 설정하는 경우
        var parent = newParent.parent
        while (parent != null) {
            if (parent.id == target.id) {
                throw IllegalArgumentException("자신의 하위 카테고리 밑으로 이동할 수 없습니다.")
            }
            parent = parent.parent
        }
    }

    /**
     * 카테고리를 삭제합니다.
     *
     * [삭제 정책: Safe Deletion]
     * 카테고리가 삭제되더라도, 해당 카테고리에 속한 게시글(Post)은 삭제되지 않아야 합니다.
     * 따라서 삭제 대상 카테고리 및 그 하위 카테고리들에 속한 모든 게시글의 category_id를
     * NULL(미분류)로 업데이트한 후, 카테고리만 물리적으로 제거합니다.
     */
    @Transactional
    fun deleteCategory(id: Long) {
        val category = categoryRepository.findByIdOrNull(id)
            ?: throw IllegalArgumentException("존재하지 않는 카테고리입니다.")

        // 삭제할 카테고리와 그 자손들을 모두 수집 (Flattening)
        val categoriesToDelete = mutableListOf<Category>()
        collectAllCategories(category, categoriesToDelete)

        // 연관된 게시글들의 카테고리 연결 해제 (Bulk Update로 성능 최적화)
        postRepository.bulkUpdateCategoryToNull(categoriesToDelete)

        // 카테고리 삭제 (Cascade 설정에 의해 하위 카테고리도 DB에서 삭제됨)
        categoryRepository.delete(category)
    }

    /**
     * 재귀적으로 하위 카테고리를 모두 순회하여 리스트에 담습니다.
     */
    private fun collectAllCategories(category: Category, list: MutableList<Category>) {
        list.add(category)
        category.children.forEach { collectAllCategories(it, list) }
    }
}