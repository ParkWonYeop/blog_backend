package me.wypark.blogbackend.domain.category

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

/**
 * [카테고리 데이터 접근 계층]
 *
 * 카테고리 엔티티의 영속성(Persistence)을 관리합니다.
 * 계층형 구조(Hierarchy)의 특성을 고려하여 N+1 문제를 방지하기 위한
 * 최적화된 Fetch Join 쿼리를 포함하고 있습니다.
 */
interface CategoryRepository : JpaRepository<Category, Long> {

    /**
     * 최상위(Root) 카테고리 목록을 조회합니다.
     *
     * [성능 최적화: Fetch Join]
     * 카테고리 트리를 구성할 때, 지연 로딩(Lazy Loading)으로 인한 N+1 문제를 방지하기 위해
     * `LEFT JOIN FETCH`를 사용하여 자식 카테고리(children)까지 한 번의 쿼리로 즉시 로딩합니다.
     * 이를 통해 애플리케이션 레벨에서 재귀적으로 트리를 구성할 때 DB 접근 횟수를 최소화합니다.
     */
    @Query("SELECT DISTINCT c FROM Category c LEFT JOIN FETCH c.children WHERE c.parent IS NULL")
    fun findAllRoots(): List<Category>

    /**
     * 카테고리 이름의 중복 여부를 검사합니다.
     *
     * 동일한 레벨 내에서 같은 이름의 카테고리가 생성되는 것을 방지하여
     * 사용자의 혼란을 막고 데이터의 유니크성을 보장하기 위해 사용됩니다.
     */
    fun existsByName(name: String): Boolean

    /**
     * 이름으로 카테고리를 조회합니다.
     *
     * 게시글 작성 시 카테고리 이름 문자열을 엔티티로 매핑하거나,
     * URL 경로(Path Variable)를 통해 카테고리를 찾을 때 활용됩니다.
     */
    fun findByName(name: String): Category?
}