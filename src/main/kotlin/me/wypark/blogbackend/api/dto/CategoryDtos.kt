package me.wypark.blogbackend.api.dto

import me.wypark.blogbackend.domain.category.Category

// [요청] 카테고리 생성
data class CategoryCreateRequest(
    val name: String,
    val parentId: Long? = null // null이면 최상위(Root) 카테고리
)

// [요청] 카테고리 수정 (이름 + 부모 이동)
data class CategoryUpdateRequest(
    val name: String,
    val parentId: Long? // null이면 최상위(Root)로 이동
)

// [응답] 카테고리 트리 구조 (재귀)
data class CategoryResponse(
    val id: Long,
    val name: String,
    val children: List<CategoryResponse> // 자식들
) {
    companion object {
        // Entity -> DTO 변환 (재귀 호출)
        fun from(category: Category): CategoryResponse {
            return CategoryResponse(
                id = category.id!!,
                name = category.name,
                // 자식들을 DTO로 변환하여 리스트에 담음
                children = category.children.map { from(it) }
            )
        }
    }
}

// [Admin용 응답] 관리자 대시보드 목록용
data class AdminCommentResponse(
    val id: Long,
    val content: String,
    val author: String,
    val postTitle: String, // 어떤 글인지 식별
    val postSlug: String,  // 클릭 시 해당 글로 이동용
    val createdAt: java.time.LocalDateTime
) {
    companion object {
        fun from(comment: me.wypark.blogbackend.domain.comment.Comment): AdminCommentResponse {
            return AdminCommentResponse(
                id = comment.id!!,
                content = comment.content,
                author = comment.getAuthorName(),
                postTitle = comment.post.title,
                postSlug = comment.post.slug,
                createdAt = comment.createdAt
            )
        }
    }
}