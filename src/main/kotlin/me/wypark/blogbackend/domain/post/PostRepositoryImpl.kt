package me.wypark.blogbackend.domain.post

import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.Order
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.Projections
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.core.types.dsl.PathBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import me.wypark.blogbackend.api.dto.PostSummaryResponse
import me.wypark.blogbackend.domain.post.QPost.post
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import me.wypark.blogbackend.domain.tag.QPostTag.postTag
import me.wypark.blogbackend.domain.tag.QTag.tag

class PostRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : PostRepositoryCustom {
    override fun search(keyword: String?, categoryName: String?, tagName: String?, pageable: Pageable): Page<PostSummaryResponse> {
        // 1. 동적 필터링 조건
        val builder = BooleanBuilder()
        builder.and(containsKeyword(keyword))
        builder.and(eqCategory(categoryName))
        builder.and(eqTagName(tagName)) // 👈 태그 조건 추가

        // 2. 쿼리 실행 (Join 추가)
        val query = queryFactory
            .select(
                Projections.constructor(
                    PostSummaryResponse::class.java,
                    post.id,
                    post.title,
                    post.slug,
                    post.category.name,
                    post.viewCount,
                    post.createdAt
                )
            )
            .from(post)
            // 👇 태그 검색을 위해 테이블 Join
            .leftJoin(post.tags, postTag)
            .leftJoin(postTag.tag, tag)
            .where(builder)
            .distinct() // ⭐ 중요: 하나의 글에 태그가 여러 개면 글이 중복 조회될 수 있어서 제거
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())

        // 3. 정렬 적용
        for (order in getOrderSpecifiers(pageable)) {
            query.orderBy(order)
        }

        val content = query.fetch()

        // 4. 전체 개수 (Count 쿼리에도 Join 필요)
        val total = queryFactory
            .select(post.countDistinct()) // ⭐ 개수 셀 때도 중복 제거
            .from(post)
            .leftJoin(post.tags, postTag)
            .leftJoin(postTag.tag, tag)
            .where(builder)
            .fetchOne() ?: 0L

        return PageImpl(content, pageable, total)
    }

    // --- 조건 메서드들 ---

    // 검색어 (제목 or 본문)
    private fun containsKeyword(keyword: String?): BooleanBuilder {
        val builder = BooleanBuilder()
        if (!keyword.isNullOrBlank()) {
            builder.or(post.title.containsIgnoreCase(keyword))
            builder.or(post.content.containsIgnoreCase(keyword))
        }
        return builder
    }

    // 카테고리 일치 (카테고리명이 없으면 무시)
    private fun eqCategory(categoryName: String?): BooleanExpression? {
        if (categoryName.isNullOrBlank()) return null
        return post.category.name.eq(categoryName)
    }

    private fun eqTagName(tagName: String?): BooleanExpression? {
        if (tagName.isNullOrBlank()) return null
        return tag.name.eq(tagName) // 태그 이름은 정확히 일치해야 함
    }

    // Pageable Sort -> QueryDSL OrderSpecifier 변환
    private fun getOrderSpecifiers(pageable: Pageable): List<OrderSpecifier<*>> {
        val orders = mutableListOf<OrderSpecifier<*>>()

        if (!pageable.sort.isEmpty) {
            for (order in pageable.sort) {
                val direction = if (order.direction.isAscending) Order.ASC else Order.DESC

                // 들어온 정렬 기준값(property)에 따라 QClass 필드 매핑
                when (order.property) {
                    "viewCount" -> orders.add(OrderSpecifier(direction, post.viewCount)) // 인기순
                    "createdAt" -> orders.add(OrderSpecifier(direction, post.createdAt)) // 최신순
                    "id" -> orders.add(OrderSpecifier(direction, post.id))
                    else -> orders.add(OrderSpecifier(Order.DESC, post.id)) // 기본
                }
            }
        }
        return orders
    }
}