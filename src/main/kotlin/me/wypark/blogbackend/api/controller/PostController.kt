package me.wypark.blogbackend.api.controller

import me.wypark.blogbackend.api.common.ApiResponse
import me.wypark.blogbackend.application.post.PostResponse
import me.wypark.blogbackend.application.post.PostService
import me.wypark.blogbackend.application.post.PostSummaryResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/posts")
class PostController(
    private val postService: PostService
) {

