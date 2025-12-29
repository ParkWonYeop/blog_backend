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
import me.wypark.blogbackend.domain.category.QCategory.category // 👈 QCategory import 추가
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable

class PostRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : PostRepositoryCustom {
    override fun search(keyword: String?, categoryNames: List<String>?, tagName: String?, pageable: Pageable): Page<PostSummaryResponse> {
        val builder = BooleanBuilder()
        builder.and(containsKeyword(keyword))

        // "uncategorized" (또는 "미분류") 요청 시 post.category.isNull 조건으로 변환하여 처리
        builder.and(inCategoryNames(categoryNames))

        builder.and(eqTagName(tagName))

        val query = queryFactory
            .select(
                Projections.constructor(
                    PostSummaryResponse::class.java,
                    post.id,
                    post.title,
                    post.slug,
                    category.name, // 👈 post.category.name 대신 alias 사용 (NULL 안전)
                    post.viewCount,
                    post.createdAt,
                    post.updatedAt,
                    post.content // 👈 [추가] 미리보기를 위해 본문 내용도 조회합니다.
                )
            )
            .from(post)
            .leftJoin(post.category, category) // 👈 명시적 Left Join 추가 (카테고리 없어도 글 조회 가능하게 함)
            .leftJoin(post.tags, postTag)
            .leftJoin(postTag.tag, tag)
            .where(builder)
            .distinct()
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())

        for (order in getOrderSpecifiers(pageable)) {
            query.orderBy(order)
        }

        val content = query.fetch()

        val total = queryFactory
            .select(post.countDistinct())
            .from(post)
            .leftJoin(post.category, category) // 👈 Count 쿼리에도 Left Join 추가
            .leftJoin(post.tags, postTag)
            .leftJoin(postTag.tag, tag)
            .where(builder)
            .fetchOne() ?: 0L

        return PageImpl(content, pageable, total)
    }

    private fun containsKeyword(keyword: String?): BooleanBuilder {
        val builder = BooleanBuilder()
        if (!keyword.isNullOrBlank()) {
            builder.or(post.title.containsIgnoreCase(keyword))
            builder.or(post.content.containsIgnoreCase(keyword))
        }
        return builder
    }

    private fun inCategoryNames(categoryNames: List<String>?): BooleanExpression? {
        if (categoryNames.isNullOrEmpty()) return null

        // 1. 요청에 "uncategorized" 또는 "미분류"가 포함되어 있는지 확인
        // (프론트엔드에서 한글로 "미분류"를 보내는 경우가 많으므로 둘 다 체크)
        val hasUncategorized = categoryNames.any {
            it.equals("uncategorized", ignoreCase = true) || it.equals("미분류", ignoreCase = true)
        }

        // 2. 그 외 일반 카테고리 이름들만 따로 추림
        val normalNames = categoryNames.filter {
            !it.equals("uncategorized", ignoreCase = true) && !it.equals("미분류", ignoreCase = true)
        }

        var expression: BooleanExpression? = null

        // A. 일반 카테고리 이름 조건 (IN 절)
        if (normalNames.isNotEmpty()) {
            expression = category.name.`in`(normalNames) // 👈 alias 사용
        }

        // B. uncategorized 조건 (IS NULL) 추가
        if (hasUncategorized) {
            val isNullExpr = post.category.isNull // FK가 NULL인지 확인

            expression = if (expression != null) {
                // (일반 카테고리들) OR (카테고리 없음) -> 둘 중 하나라도 만족하면 조회
                expression.or(isNullExpr)
            } else {
                // (카테고리 없음) -> 카테고리 없는 글만 모아서 조회
                isNullExpr
            }
        }

        return expression
    }

    private fun eqTagName(tagName: String?): BooleanExpression? {
        if (tagName.isNullOrBlank()) return null
        return tag.name.eq(tagName)
    }

    private fun getOrderSpecifiers(pageable: Pageable): List<OrderSpecifier<*>> {
        val orders = mutableListOf<OrderSpecifier<*>>()

        if (!pageable.sort.isEmpty) {
            for (order in pageable.sort) {
                val direction = if (order.direction.isAscending) Order.ASC else Order.DESC
                when (order.property) {
                    "viewCount" -> orders.add(OrderSpecifier(direction, post.viewCount))
                    "createdAt" -> orders.add(OrderSpecifier(direction, post.createdAt))
                    "id" -> orders.add(OrderSpecifier(direction, post.id))
                    else -> orders.add(OrderSpecifier(Order.DESC, post.id))
                }
            }
        }
        return orders
    }
}