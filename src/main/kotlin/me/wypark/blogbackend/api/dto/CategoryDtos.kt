package me.wypark.blogbackend.api.dto

import me.wypark.blogbackend.domain.category.Category

/**
 * [카테고리 생성 요청 DTO]
 *
 * 새로운 카테고리 노드(Node)를 생성하기 위한 요청 객체입니다.
 * 계층형 게시판 구조를 지원하기 위해 부모 카테고리 ID(parentId)를 선택적으로 받습니다.
 *
 * @property parentId null일 경우 최상위(Root) 레벨에 생성되며, 값이 있을 경우 해당 카테고리의 하위(Child)로 연결됩니다.
 */
data class CategoryCreateRequest(
    val name: String,
    val parentId: Long? = null
)

/**
 * [카테고리 수정 요청 DTO]
 *
 * 카테고리의 속성 변경(Rename)과 구조 변경(Move)을 동시에 처리하는 객체입니다.
 *
 * Note:
 * 트리 구조 내에서의 노드 이동(Move)은 데이터베이스 부하가 발생할 수 있고
 * 순환 참조(Cycle) 위험이 있으므로, 서비스 레이어에서 별도의 정합성 검증 로직을 거칩니다.
 */
data class CategoryUpdateRequest(
    val name: String,
    val parentId: Long? // 변경할 부모 ID (null이면 최상위로 이동)
)

/**
 * [카테고리 응답 DTO - Tree Structure]
 *
 * 프론트엔드 네비게이션 바(Sidebar) 등에서 계층형 메뉴를 렌더링하기 위한 재귀적 구조의 객체입니다.
 *
 * [성능 고려사항]
 * N+1 문제를 방지하기 위해, 엔티티 조회 시점에는 Fetch Join을 사용하거나
 * Batch Size를 설정하여 쿼리를 최적화한 후, 메모리 상에서 이 DTO 구조로 변환하여 반환합니다.
 */
data class CategoryResponse(
    val id: Long,
    val name: String,
    val children: List<CategoryResponse> // 자식 노드 리스트 (Recursive)
) {
    companion object {
        // 엔티티 그래프를 순회하며 DTO 트리로 변환
        fun from(category: Category): CategoryResponse {
            return CategoryResponse(
                id = category.id!!,
                name = category.name,
                // 자식 카테고리들을 재귀적으로 DTO 변환하여 리스트에 매핑
                children = category.children.map { from(it) }
            )
        }
    }
}

/**
 * [관리자용 댓글 모니터링 DTO - Flat List]
 *
 * 관리자 대시보드에서 최근 댓글 흐름을 파악하기 위한 객체입니다.
 * 계층형 구조(Nested)가 필요한 일반 사용자 뷰와 달리, 관리 목적상 시간순 나열이 중요하므로
 * 모든 댓글을 평탄화(Flatten)하여 게시글 정보와 함께 제공합니다.
 */
data class AdminCommentResponse(
    val id: Long,
    val content: String,
    val author: String,
    val postTitle: String, // 문맥 파악을 위한 원본 게시글 제목
    val postSlug: String,  // 클릭 시 해당 게시글로 바로 이동(Deep Link)하기 위한 식별자
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