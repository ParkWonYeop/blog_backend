package me.wypark.blogbackend.domain.post

import me.wypark.blogbackend.api.dto.PostSummaryResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface PostRepositoryCustom {
    // categoryName(String) -> categoryNames(List<String>) 변경
    fun search(keyword: String?, categoryNames: List<String>?, tagName: String?, pageable: Pageable): Page<PostSummaryResponse>
}