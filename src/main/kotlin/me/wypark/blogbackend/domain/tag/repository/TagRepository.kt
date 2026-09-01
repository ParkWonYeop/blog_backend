package me.wypark.blogbackend.domain.tag.repository
import me.wypark.blogbackend.domain.tag.entity.Tag

import org.springframework.data.jpa.repository.JpaRepository

interface TagRepository : JpaRepository<Tag, Long> {
    fun findByName(name: String): Tag?
}