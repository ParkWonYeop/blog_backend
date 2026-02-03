package me.wypark.blogbackend.domain.post

import me.wypark.blogbackend.api.dto.PostSummaryResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/**
 * [게시글 동적 쿼리(Dynamic Query) 인터페이스]
 *
 * QueryDSL을 기반으로 복잡한 검색 및 필터링 로직을 수행하기 위한 커스텀 리포지토리 인터페이스입니다.
 * 정적 메서드(Method Name Query)만으로는 처리하기 힘든 다중 조건 조합과
 * DTO 프로젝션(Projection)을 담당합니다.
 */
interface PostRepositoryCustom {

    /**
     * 게시글을 다양한 조건으로 검색하고 페이징 처리된 요약 정보를 반환합니다.
     *
     * [검색 필터 전략]
     * - Keyword: 제목(Title)과 본문(Content)에 대한 통합 검색을 수행합니다.
     * - Categories: 단일 카테고리가 아닌 다중 카테고리 필터링(IN절)을 지원하여,
     * 사용자가 원하는 주제들을 한 번에 모아볼 수 있는 유연성을 제공합니다.
     * - Tag: 특정 태그가 포함된 게시글을 필터링합니다.
     *
     * [성능 최적화: Projection]
     * 엔티티 전체를 조회하는 대신, 목록 화면에 필요한 필드만 선별하여 DTO로 즉시 변환합니다.
     * 이는 불필요한 데이터 전송(Network I/O)을 줄이고 영속성 컨텍스트의 부하를 최소화합니다.
     */
    fun search(
        keyword: String?,
        categoryNames: List<String>?, // 다중 선택 지원 (IN Clause)
        tagName: String?,
        pageable: Pageable
    ): Page<PostSummaryResponse>
}