package me.wypark.blogbackend.domain.category

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface CategoryRepository : JpaRepository<Category, Long> {

    // 부모가 없는 최상위 카테고리들만 조회 (이걸 가져오면 자식들은 줄줄이 딸려옵니다)
    @Query("SELECT DISTINCT c FROM Category c LEFT JOIN FETCH c.children WHERE c.parent IS NULL")
    fun findAllRoots(): List<Category>

    // 카테고리 이름 중복 검사 (같은 레벨에서 중복 방지용)
    fun existsByName(name: String): Boolean

    // 이름으로 찾기 (게시글 작성 시 필요)
    fun findByName(name: String): Category?
}