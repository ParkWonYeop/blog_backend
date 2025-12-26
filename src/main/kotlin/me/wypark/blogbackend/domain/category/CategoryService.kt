package me.wypark.blogbackend.domain.category

import me.wypark.blogbackend.api.dto.CategoryCreateRequest
import me.wypark.blogbackend.api.dto.CategoryResponse
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CategoryService(
    private val categoryRepository: CategoryRepository
) {

    /**
     * [Public] 카테고리 트리 조회
     * 최상위(Root)만 조회하면, Entity 설정을 통해 자식들도 딸려옵니다.
     */
    fun getCategoryTree(): List<CategoryResponse> {
        val roots = categoryRepository.findAllRoots()
        return roots.map { CategoryResponse.from(it) }
    }

    /**
     * [Admin] 카테고리 생성
     */
    @Transactional
    fun createCategory(request: CategoryCreateRequest): Long {
        // 이름 중복 체크 (선택 사항이지만 추천)
        if (categoryRepository.existsByName(request.name)) {
            throw IllegalArgumentException("이미 존재하는 카테고리 이름입니다.")
        }

        // 부모 카테고리 확인
        val parent = request.parentId?.let {
            categoryRepository.findByIdOrNull(it)
                ?: throw IllegalArgumentException("부모 카테고리가 존재하지 않습니다.")
        }

        // 카테고리 생성
        val category = Category(
            name = request.name,
            parent = parent
        )

        // 부모와 연결 (연관관계 편의 메서드 활용)
        parent?.addChild(category)

        return categoryRepository.save(category).id!!
    }

    /**
     * [Admin] 카테고리 삭제 (선택 구현)
     * 자식이 있는 카테고리를 지울 때 어떻게 할지(전부 삭제? 연결 해제?) 정책 결정 필요
     * 여기서는 일단 간단하게 id로 삭제만 구현합니다.
     */
    @Transactional
    fun deleteCategory(id: Long) {
        categoryRepository.deleteById(id)
    }
}