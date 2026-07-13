package me.wypark.blogbackend.application.category

import me.wypark.blogbackend.domain.category.Category

data class CategoryCreateRequest(
    val name: String,
    val parentId: Long? = null
)

data class CategoryUpdateRequest(
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
