package me.wypark.blogbackend.domain.profile

import org.springframework.data.jpa.repository.JpaRepository

interface BlogProfileRepository : JpaRepository<BlogProfile, Long> {
    fun findFirstByOrderByIdAsc(): BlogProfile?
}
