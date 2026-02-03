package me.wypark.blogbackend.domain.post

import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.Order
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.Projections
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import me.wypark.blogbackend.api.dto.PostSummaryResponse
import me.wypark.blogbackend.domain.post.QPost.post
import me.wypark.blogbackend.domain.tag.QPostTag.postTag
import me.wypark.blogbackend.domain.tag.QTag.tag
import me.wypark.blogbackend.domain.category.QCategory.category
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable

/**
 * [QueryDSL 리포지토리 구현체]
 *
 * PostRepositoryCustom 인터페이스를 구현하여 복잡한 동적 쿼리를 처리합니다.
 * 컴파일 타임에 문법 오류를 잡을 수 있는 QueryDSL을 사용하여,
 * 다중 필터링 조건과 조인(Join) 로직을 안전하고 직관적으로 작성했습니다.
 */
class PostRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : PostRepositoryCustom {

    /**
     * 동적 검색 및 목록 조회
     *
     * [성능 최적화: Projections]
     * 엔티티를 통째로 조회하면 불필요한 컬럼(LOB 데이터 등)까지 로딩되어 메모리 낭비가 발생합니다.
     * 따라서 목록 화면 렌더링에 필요한 필드만 선별하여 DTO로 즉시 매핑(Projection)했습니다.
     *
     * [조회 정합성 보장]
     * - Left Join: 카테고리나 태그가 없는 게시글도 누락 없이 조회되도록 Inner Join 대신 Left Join을 사용했습니다.
     * - Distinct: 1:N 관계인 태그 테이블과 조인 시 게시글 데이터가 뻥튀기(Duplication)되는 문제를 해결합니다.
     */
    override fun search(keyword: String?, categoryNames: List<String>?, tagName: String?, pageable: Pageable): Page<PostSummaryResponse> {
        val builder = BooleanBuilder()
        builder.and(containsKeyword(keyword))
        builder.and(inCategoryNames(categoryNames))
        builder.and(eqTagName(tagName))

        val query = queryFactory
            .select(
                Projections.constructor(
                    PostSummaryResponse::class.java,
                    post.id,
                    post.title,
                    post.slug,
                    category.name, // QCategory Alias 사용으로 Null-Safe 처리
                    post.viewCount,
                    post.createdAt,
                    post.updatedAt,
                    post.content // 본문 미리보기용 데이터
                )
            )
            .from(post)
            .leftJoin(post.category, category) // 카테고리 미지정 글 포함
            .leftJoin(post.tags, postTag)      // 태그 미지정 글 포함
            .leftJoin(postTag.tag, tag)
            .where(builder)
            .distinct()
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())

        // 동적 정렬 적용
        for (order in getOrderSpecifiers(pageable)) {
            query.orderBy(order)
        }

        val content = query.fetch()

        // [Count 쿼리 분리]
        // 페이징을 위한 전체 개수 조회 시, 데이터 조회 쿼리보다 단순화할 수 있는 여지가 있다면
        // 별도의 쿼리로 분리하여 성능을 최적화하는 것이 좋습니다.
        val total = queryFactory
            .select(post.countDistinct())
            .from(post)
            .leftJoin(post.category, category)
            .leftJoin(post.tags, postTag)
            .leftJoin(postTag.tag, tag)
            .where(builder)
            .fetchOne() ?: 0L

        return PageImpl(content, pageable, total)
    }

    private fun containsKeyword(keyword: String?): BooleanBuilder {
        val builder = BooleanBuilder()
        if (!keyword.isNullOrBlank()) {
            // 제목 또는 본문에 키워드가 포함되는지 검사 (OR 조건)
            builder.or(post.title.containsIgnoreCase(keyword))
            builder.or(post.content.containsIgnoreCase(keyword))
        }
        return builder
    }

    /**
     * 카테고리 다중 필터링 조건 생성
     *
     * [미분류(Uncategorized) 처리 전략]
     * 클라이언트로부터 "uncategorized" 요청이 오면 DB상의 NULL 값과 매핑해야 합니다.
     * 일반 카테고리(IN 절)와 미분류(IS NULL) 조건이 혼재될 경우, 이를 유연하게 OR 연산으로 묶어 처리합니다.
     */
    private fun inCategoryNames(categoryNames: List<String>?): BooleanExpression? {
        if (categoryNames.isNullOrEmpty()) return null

        // 1. 특수 키워드 체크 ("uncategorized", "미분류")
        val hasUncategorized = categoryNames.any {
            it.equals("uncategorized", ignoreCase = true) || it.equals("미분류", ignoreCase = true)
        }

        // 2. 일반 카테고리명 추출
        val normalNames = categoryNames.filter {
            !it.equals("uncategorized", ignoreCase = true) && !it.equals("미분류", ignoreCase = true)
        }

        var expression: BooleanExpression? = null

        // A. 일반 카테고리 조건 (IN Clause)
        if (normalNames.isNotEmpty()) {
            expression = category.name.`in`(normalNames)
        }

        // B. 미분류 조건 (IS NULL) 결합
        if (hasUncategorized) {
            val isNullExpr = post.category.isNull

            expression = if (expression != null) {
                // (일반 카테고리들) OR (미분류) -> 둘 중 하나라도 만족하면 조회
                expression.or(isNullExpr)
            } else {
                // 오직 미분류 글만 조회
                isNullExpr
            }
        }

        return expression
    }

    private fun eqTagName(tagName: String?): BooleanExpression? {
        if (tagName.isNullOrBlank()) return null
        return tag.name.eq(tagName)
    }

    /**
     * Pageable의 Sort 객체를 QueryDSL의 OrderSpecifier로 변환
     * 문자열 필드명을 실제 Q-Type 필드로 매핑하여 런타임 에러를 방지합니다.
     */
    private fun getOrderSpecifiers(pageable: Pageable): List<OrderSpecifier<*>> {
        val orders = mutableListOf<OrderSpecifier<*>>()

        if (!pageable.sort.isEmpty) {
            for (order in pageable.sort) {
                val direction = if (order.direction.isAscending) Order.ASC else Order.DESC
                when (order.property) {
                    "viewCount" -> orders.add(OrderSpecifier(direction, post.viewCount))
                    "createdAt" -> orders.add(OrderSpecifier(direction, post.createdAt))
                    "id" -> orders.add(OrderSpecifier(direction, post.id))
                    else -> orders.add(OrderSpecifier(Order.DESC, post.id)) // 기본 정렬
                }
            }
        }
        return orders
    }
}