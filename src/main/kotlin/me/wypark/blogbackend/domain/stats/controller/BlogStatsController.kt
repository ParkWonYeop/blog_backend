package me.wypark.blogbackend.domain.stats.controller

import me.wypark.blogbackend.domain.stats.service.BlogStatsService
import me.wypark.blogbackend.domain.stats.service.BlogStatsSummary
import me.wypark.blogbackend.global.common.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/stats")
class BlogStatsController(
    private val blogStatsService: BlogStatsService
) {

    @GetMapping("/summary")
    fun summary(): ResponseEntity<ApiResponse<BlogStatsSummary>> {
        return ResponseEntity.ok(ApiResponse.success(blogStatsService.summary()))
    }
}
