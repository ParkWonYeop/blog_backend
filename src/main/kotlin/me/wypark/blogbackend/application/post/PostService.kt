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

    @Transactional
    fun getPostBySlug(slug: String): PostResponse {
        val post = postRepository.findBySlug(slug)
            ?: throw BusinessException("해당 게시글을 찾을 수 없습니다: $slug")
        val postId = requireNotNull(post.id) { "Persisted post must have an id" }

        post.increaseViewCount()
        val viewDate = LocalDate.now(clock.withZone(KOREA_ZONE_ID))
        postViewCounter.increment(postId, viewDate)

        val previous = postRepository.findFirstByIdLessThanOrderByIdDesc(postId)
        val next = postRepository.findFirstByIdGreaterThanOrderByIdAsc(postId)
        return PostResponse.from(post, previous, next)
    }

    @Transactional
    fun createPost(request: PostSaveRequest, email: String): Long {
        val member = memberRepository.findByEmail(email)
            ?: throw BusinessException("회원 없음")
        val category = request.categoryId?.let(categoryRepository::findByIdOrNull)
        val slug = uniqueSlug(request.slug, request.title)

        val post = Post(
            title = request.title,
            content = request.content,
            slug = slug,
            member = member,
            category = category
        )
        post.addTags(resolveTags(request.tags, post))

        return requireNotNull(postRepository.save(post).id) { "Saved post must have an id" }
    }

    @Transactional
    fun updatePost(id: Long, request: PostSaveRequest): Long {
        val post = postRepository.findByIdOrNull(id)
            ?: throw BusinessException("존재하지 않는 게시글입니다.")

        deleteRemovedImages(post.content, request.content)

        val category = request.categoryId?.let(categoryRepository::findByIdOrNull)
        val slug = request.slug
            ?.takeUnless(String::isBlank)
            ?.takeIf { it != post.slug }
            ?.let { uniqueSlug(it, request.title) }
            ?: post.slug

        post.update(request.title, request.content, slug, category)
        post.updateTags(resolveTags(request.tags, post))
        return requireNotNull(post.id) { "Persisted post must have an id" }
    }

    @Transactional
    fun deletePost(id: Long) {
        val post = postRepository.findByIdOrNull(id)
            ?: throw BusinessException("존재하지 않는 게시글입니다.")

        MarkdownImageExtractor.extractFileNames(post.content).forEach(imageService::deleteImage)
        postRepository.delete(post)
    }

    fun searchPosts(
        keyword: String?,
        categoryName: String?,
        tagName: String?,
        pageable: Pageable
    ): Page<PostSummaryResponse> {
        val categoryNames = categoryName?.let(::getCategoryAndDescendants)
        return postRepository.search(keyword, categoryNames, tagName, pageable)
            .map(PostSummaryResponse::from)
    }

