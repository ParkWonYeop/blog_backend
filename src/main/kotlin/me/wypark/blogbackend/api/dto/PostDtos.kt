package me.wypark.blogbackend.api.dto

import me.wypark.blogbackend.domain.post.Post
import java.time.LocalDateTime

/**
 * [인접 게시글 응답 DTO]
 *
 * 게시글 상세 화면 하단에 위치할 '이전 글 / 다음 글' 네비게이션 링크를 위한 객체입니다.
 * 전체 데이터를 로딩하는 대신, 링크 생성에 필요한 최소한의 식별자(Slug)와 제목(Title)만 포함하여
 * 페이로드 크기를 최적화했습니다.
 */
data class PostNeighborResponse(
    val slug: String,
    val title: String
) {
    companion object {
        fun from(post: Post): PostNeighborResponse {
            return PostNeighborResponse(
                slug = post.slug,
                title = post.title
            )
        }
    }
}

/**
 * [게시글 상세 응답 DTO]
 *
 * 단일 게시글의 모든 정보(Full Content)를 클라이언트에게 전달하는 객체입니다.
 *
 * [설계 의도]
 * - SEO: ID 대신 Slug를 사용하여 검색 엔진 친화적인 URL 구조 지원
 * - UX: 별도의 추가 요청 없이 이전/다음 글 정보를 함께 반환하여 페이지 이동성(Navigability) 향상
 */
data class PostResponse(
    val id: Long,
    val title: String,
    val content: String,
    val slug: String,
    val categoryName: String?,
    val viewCount: Long,
    val createdAt: LocalDateTime,

    // 현재 글을 기준으로 앞/뒤 글 정보 (없으면 null)
    val prevPost: PostNeighborResponse?,
    val nextPost: PostNeighborResponse?
) {
    companion object {
        fun from(post: Post, prevPost: Post? = null, nextPost: Post? = null): PostResponse {
            return PostResponse(
                id = post.id!!,
                title = post.title,
                content = post.content,
                slug = post.slug,
                categoryName = post.category?.name,
                viewCount = post.viewCount,
                createdAt = post.createdAt,
                prevPost = prevPost?.let { PostNeighborResponse.from(it) },
                nextPost = nextPost?.let { PostNeighborResponse.from(it) }
            )
        }
    }
}

/**
 * [게시글 목록 응답 DTO]
 *
 * 메인 화면이나 카테고리 목록에서 사용되는 경량화(Lightweight) 객체입니다.
 *
 * [최적화 전략]
 * 다수의 아이템을 렌더링해야 하므로, 데이터 전송량(Network Overhead)을 줄이기 위해
 * 무거운 본문(content)은 제외하거나 미리보기용으로 일부만 포함하도록 설계되었습니다.
 */
data class PostSummaryResponse(
    val id: Long,
    val title: String,
    val slug: String,
    val categoryName: String?,
    val viewCount: Long,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val content: String? // 목록에서는 본문 미리보기 용도로 사용 (혹은 null)
) {
    companion object {
        fun from(post: Post): PostSummaryResponse {
            return PostSummaryResponse(
                id = post.id!!,
                title = post.title,
                slug = post.slug,
                categoryName = post.category?.name,
                viewCount = post.viewCount,
                createdAt = post.createdAt,
                updatedAt = post.updatedAt,
                content = post.content
            )
        }
    }
}

/**
 * [게시글 작성/수정 요청 DTO]
 *
 * 게시글의 생명주기(생성/수정)를 담당하는 통합 커맨드 객체입니다.
 *
 * - Slug: 클라이언트가 직접 지정하지 않으면(null), 서버에서 제목을 기반으로 자동 생성(Generate)합니다.
 * - Content: Markdown 포맷의 원문 텍스트를 저장합니다.
 */
data class PostSaveRequest(
    val title: String,
    val content: String,
    val slug: String? = null,
    val categoryId: Long? = null,
    val tags: List<String> = emptyList() // 태그는 서비스 레이어에서 별도 로직으로 매핑(Many-to-Many) 처리
)