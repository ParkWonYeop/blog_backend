package me.wypark.blogbackend.domain.category.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import me.wypark.blogbackend.domain.category.entity.Category

data class CategoryCreateRequest(
    @field:NotBlank(message = "카테고리 이름은 필수입니다.")
    @field:Size(max = 100, message = "카테고리 이름은 100자 이하로 입력해주세요.")
    val name: String,
    val parentId: Long? = null
)

data class CategoryUpdateRequest(
    @field:NotBlank(message = "카테고리 이름은 필수입니다.")
    @field:Size(max = 100, message = "카테고리 이름은 100자 이하로 입력해주세요.")
    val name: String,
    val parentId: Long?
)

data class CategoryResponse(
    val id: Long,
    val name: String,
    val children: List<CategoryResponse>
) {
    companion object {
        fun from(category: Category): CategoryResponse {
            return CategoryResponse(
                id = requireNotNull(category.id) { "Persisted category must have an id" },
                name = category.name,
                children = category.children.map(::from)
            )
        }
    }
}
