package me.wypark.blogbackend.application.post

import me.wypark.blogbackend.application.common.BusinessException
import me.wypark.blogbackend.domain.category.Category
import me.wypark.blogbackend.domain.category.CategoryRepository
import me.wypark.blogbackend.application.image.ImageService
import me.wypark.blogbackend.domain.post.Post
import me.wypark.blogbackend.domain.post.PostRepository
import me.wypark.blogbackend.domain.tag.PostTag
import me.wypark.blogbackend.domain.tag.Tag
import me.wypark.blogbackend.domain.tag.TagRepository
import me.wypark.blogbackend.domain.user.MemberRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

@Service
@Transactional(readOnly = true)
class PostService(
    private val postRepository: PostRepository,
    private val categoryRepository: CategoryRepository,
    private val memberRepository: MemberRepository,
    private val tagRepository: TagRepository,
    private val imageService: ImageService,
    private val postViewCounter: PostViewCounter,
    private val clock: Clock
) {

    fun getPosts(pageable: Pageable): Page<PostSummaryResponse> {
        return postRepository.findAll(pageable).map(PostSummaryResponse::from)
    }

