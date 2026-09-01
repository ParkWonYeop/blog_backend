package me.wypark.blogbackend.domain.profile.repository
import me.wypark.blogbackend.domain.profile.entity.BlogProfile

import org.springframework.data.jpa.repository.JpaRepository

interface BlogProfileRepository : JpaRepository<BlogProfile, Long> {
    fun findFirstByOrderByIdAsc(): BlogProfile?
}
