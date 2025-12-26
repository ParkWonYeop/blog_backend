package me.wypark.blogbackend.domain.post

import me.wypark.blogbackend.api.dto.PostSummaryResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface PostRepositoryCustom {
    fun search(keyword: String?, categoryName: String?, tagName: String?, pageable: Pageable): Page<PostSummaryResponse>
}