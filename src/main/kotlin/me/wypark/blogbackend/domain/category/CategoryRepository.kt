package me.wypark.blogbackend.domain.category

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface CategoryRepository : JpaRepository<Category, Long> {

    @Query("SELECT DISTINCT c FROM Category c LEFT JOIN FETCH c.children WHERE c.parent IS NULL")
    fun findAllRoots(): List<Category>

    fun existsByName(name: String): Boolean

    fun findByName(name: String): Category?
}