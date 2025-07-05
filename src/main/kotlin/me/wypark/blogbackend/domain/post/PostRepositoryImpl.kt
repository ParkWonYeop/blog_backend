package me.wypark.blogbackend.domain.post

import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.Order
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.Projections
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import me.wypark.blogbackend.domain.post.QPost.post
import me.wypark.blogbackend.domain.category.QCategory.category
import me.wypark.blogbackend.domain.tag.QPostTag.postTag
import me.wypark.blogbackend.domain.tag.QTag.tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable

class PostRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : PostRepositoryCustom {

    override fun search(
        keyword: String?,
        categoryNames: List<String>?,
        tagName: String?,
        pageable: Pageable
    ): Page<PostSummary> {
        val predicate = BooleanBuilder()
            .and(containsKeyword(keyword))
            .and(inCategoryNames(categoryNames))
            .and(eqTagName(tagName))

        val query = queryFactory
            .select(
                Projections.constructor(
                    PostSummary::class.java,
                    post.id,
                    post.title,
                    post.slug,
                    category.name,
                    post.viewCount,
                    post.createdAt,
                    post.updatedAt,
                    post.content
                )
            )
            .from(post)
            .leftJoin(post.category, category)
            .leftJoin(post.tags, postTag)
            .leftJoin(postTag.tag, tag)
            .where(predicate)
            .distinct()
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())

        query.orderBy(*orderSpecifiers(pageable).toTypedArray())

        val content = query.fetch()

        val total = queryFactory
            .select(post.countDistinct())
            .from(post)
            .leftJoin(post.category, category)
            .leftJoin(post.tags, postTag)
            .leftJoin(postTag.tag, tag)
            .where(predicate)
            .fetchOne() ?: 0L

        return PageImpl(content, pageable, total)
    }

    private fun containsKeyword(keyword: String?): BooleanExpression? {
        if (keyword.isNullOrBlank()) return null
        return post.title.containsIgnoreCase(keyword)
            .or(post.content.containsIgnoreCase(keyword))
    }

    private fun inCategoryNames(categoryNames: List<String>?): BooleanExpression? {
        if (categoryNames.isNullOrEmpty()) return null

        val hasUncategorized = categoryNames.any(::isUncategorized)
        val normalNames = categoryNames.filterNot(::isUncategorized)

        var expression: BooleanExpression? = null

        if (normalNames.isNotEmpty()) {
            expression = category.name.`in`(normalNames)
        }

        if (hasUncategorized) {
            expression = if (expression != null) {
                expression.or(post.category.isNull)
            } else {
                post.category.isNull
            }
        }

        return expression
    }

    private fun eqTagName(tagName: String?): BooleanExpression? {
        if (tagName.isNullOrBlank()) return null
        return tag.name.eq(tagName)
    }

    private fun orderSpecifiers(pageable: Pageable): List<OrderSpecifier<*>> {
        return pageable.sort.map { order ->
            val direction = if (order.isAscending) Order.ASC else Order.DESC
            when (order.property) {
                "viewCount" -> OrderSpecifier(direction, post.viewCount)
                "createdAt" -> OrderSpecifier(direction, post.createdAt)
                "id" -> OrderSpecifier(direction, post.id)
                else -> OrderSpecifier(Order.DESC, post.id)
            }
        }.toList()
    }

    private fun isUncategorized(name: String): Boolean {
        return UNCATEGORIZED_NAMES.any { it.equals(name, ignoreCase = true) }
    }

    companion object {
        private val UNCATEGORIZED_NAMES = setOf("uncategorized", "미분류")
    }
}
