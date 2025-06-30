package me.wypark.blogbackend.domain.post

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface PostRepositoryCustom {

    fun search(
        keyword: String?,
        categoryNames: List<String>?,
        tagName: String?,
        pageable: Pageable
    ): Page<PostSummary>
}
