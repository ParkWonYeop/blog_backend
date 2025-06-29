package me.wypark.blogbackend.domain.post

import me.wypark.blogbackend.domain.category.Category
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PostRepository : JpaRepository<Post, Long>, PostRepositoryCustom {

    fun findBySlug(slug: String): Post?

    fun existsBySlug(slug: String): Boolean

    override fun findAll(pageable: Pageable): Page<Post>

    fun findAllByCategory(category: Category, pageable: Pageable): Page<Post>

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Post p SET p.category = null WHERE p.category IN :categories")
    fun bulkUpdateCategoryToNull(@Param("categories") categories: List<Category>)

    fun findFirstByIdLessThanOrderByIdDesc(id: Long): Post?

    fun findFirstByIdGreaterThanOrderByIdAsc(id: Long): Post?
}