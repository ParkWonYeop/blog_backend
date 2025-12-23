package me.wypark.blogbackend.application.post

import me.wypark.blogbackend.domain.category.Category
import me.wypark.blogbackend.domain.category.CategoryRepository
import me.wypark.blogbackend.domain.post.Post
import me.wypark.blogbackend.domain.post.PostRepository
import me.wypark.blogbackend.domain.user.Member
import me.wypark.blogbackend.domain.user.MemberRepository
import me.wypark.blogbackend.domain.user.Role
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertEquals

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PostSearchIntegrationTest {

    @Autowired
    lateinit var postService: PostService

    @Autowired
    lateinit var memberRepository: MemberRepository

    @Autowired
    lateinit var categoryRepository: CategoryRepository

    @Autowired
    lateinit var postRepository: PostRepository

