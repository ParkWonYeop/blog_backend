package me.wypark.blogbackend.domain.post

import me.wypark.blogbackend.domain.category.Category
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

/**
 * [게시글 데이터 접근 계층]
 *
 * 게시글(Post) 엔티티의 영속성을 관리하며,
 * 검색(Search), 필터링(Filter), 대량 수정(Bulk Update) 등의 다양한 DB 조작을 수행합니다.
 * 복잡한 동적 쿼리는 PostRepositoryCustom(QueryDSL)을 통해 처리합니다.
 */
interface PostRepository : JpaRepository<Post, Long>, PostRepositoryCustom {

    /**
     * URL 친화적인 식별자(Slug)로 게시글을 단건 조회합니다.
     * 숫자 ID 대신 의미 있는 문자열을 사용하여 검색 엔진 최적화(SEO)와 사용자 경험(UX)을 향상시킵니다.
     */
    fun findBySlug(slug: String): Post?

    /**
     * Slug의 유일성(Uniqueness)을 검증합니다.
     * 게시글 작성/수정 시 중복된 Slug가 발생하지 않도록 사전에 확인하는 용도입니다.
     */
    fun existsBySlug(slug: String): Boolean

    /**
     * 기본 페이징 조회 메서드를 오버라이드합니다.
     * 최신순, 조회순 등 다양한 정렬 기준은 Pageable 객체에 담겨 전달됩니다.
     */
    override fun findAll(pageable: Pageable): Page<Post>

    /**
     * 특정 카테고리에 속한 게시글 목록을 페이징하여 조회합니다.
     */
    fun findAllByCategory(category: Category, pageable: Pageable): Page<Post>

    /**
     * [벌크 연산 최적화]
     *
     * 카테고리 삭제 시, 해당 카테고리에 속했던 게시글들을 일일이 조회하여 수정(Dirty Checking)하는 것은 비효율적입니다.
     * 따라서 단 한 번의 UPDATE 쿼리로 '미분류(NULL)' 처리를 수행하여 성능을 극대화합니다.
     *
     * @Modifying(clearAutomatically = true):
     * 벌크 연산은 영속성 컨텍스트를 무시하고 DB에 직접 쿼리를 날리므로,
     * 실행 후 1차 캐시와 DB의 데이터 불일치를 막기 위해 자동으로 캐시를 비웁니다.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Post p SET p.category = null WHERE p.category IN :categories")
    fun bulkUpdateCategoryToNull(@Param("categories") categories: List<Category>)

    /**
     * [이전 글 조회]
     * 현재 글(ID)보다 작으면서(Less Than) 가장 큰 ID를 가진 레코드를 찾습니다.
     * (즉, 바로 직전에 작성된 글)
     */
    fun findFirstByIdLessThanOrderByIdDesc(id: Long): Post?

    /**
     * [다음 글 조회]
     * 현재 글(ID)보다 크면서(Greater Than) 가장 작은 ID를 가진 레코드를 찾습니다.
     * (즉, 바로 직후에 작성된 글)
     */
    fun findFirstByIdGreaterThanOrderByIdAsc(id: Long): Post?
}