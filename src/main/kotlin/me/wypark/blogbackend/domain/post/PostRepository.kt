package me.wypark.blogbackend.domain.post

import me.wypark.blogbackend.domain.category.Category
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface PostRepository : JpaRepository<Post, Long>, PostRepositoryCustom{

    // 1. Slug로 상세 조회 (URL이 깔끔해짐)
    fun findBySlug(slug: String): Post?

    // 2. Slug 중복 검사 (글 작성/수정 시 필수)
    fun existsBySlug(slug: String): Boolean

    // 3. 페이징된 목록 조회 (최신순 등은 Pageable로 해결)
    override fun findAll(pageable: Pageable): Page<Post>

    // 4. 특정 카테고리의 글 목록 조회
    fun findAllByCategory(category: Category, pageable: Pageable): Page<Post>
}