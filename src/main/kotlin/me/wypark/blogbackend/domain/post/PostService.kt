package me.wypark.blogbackend.domain.post

import me.wypark.blogbackend.api.dto.PostResponse
import me.wypark.blogbackend.api.dto.PostSaveRequest
import me.wypark.blogbackend.api.dto.PostSummaryResponse
import me.wypark.blogbackend.domain.category.Category
import me.wypark.blogbackend.domain.category.CategoryRepository
import me.wypark.blogbackend.domain.image.ImageService
import me.wypark.blogbackend.domain.tag.PostTag
import me.wypark.blogbackend.domain.tag.Tag
import me.wypark.blogbackend.domain.tag.TagRepository
import me.wypark.blogbackend.domain.user.MemberRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PostService(
    private val postRepository: PostRepository,
    private val categoryRepository: CategoryRepository,
    private val memberRepository: MemberRepository,
    private val tagRepository: TagRepository,
    private val imageService: ImageService
) {

    fun getPosts(pageable: Pageable): Page<PostSummaryResponse> {
        return postRepository.findAll(pageable)
            .map { PostSummaryResponse.from(it) }
    }

    @Transactional
    fun getPostBySlug(slug: String): PostResponse {
        val post = postRepository.findBySlug(slug)
            ?: throw IllegalArgumentException("해당 게시글을 찾을 수 없습니다: $slug")

        post.increaseViewCount()

        // 👈 [추가] 이전/다음 게시글 조회
        // prevPost: 현재 글보다 ID가 작으면서 가장 가까운 글 (과거 글)
        val prevPost = postRepository.findFirstByIdLessThanOrderByIdDesc(post.id!!)
        // nextPost: 현재 글보다 ID가 크면서 가장 가까운 글 (최신 글)
        val nextPost = postRepository.findFirstByIdGreaterThanOrderByIdAsc(post.id!!)

        return PostResponse.from(post, prevPost, nextPost)
    }

    @Transactional
    fun createPost(request: PostSaveRequest, email: String): Long {
        val member = memberRepository.findByEmail(email)
            ?: throw IllegalArgumentException("회원 없음")

        val category = request.categoryId?.let { categoryRepository.findByIdOrNull(it) }

        // Slug 생성 로직
        val uniqueSlug = generateUniqueSlug(request.slug, request.title)

        val post = Post(
            title = request.title,
            content = request.content,
            slug = uniqueSlug,
            member = member,
            category = category
        )

        val postTags = resolveTags(request.tags, post)
        post.addTags(postTags)

        return postRepository.save(post).id!!
    }

    // 게시글 수정
    @Transactional
    fun updatePost(id: Long, request: PostSaveRequest): Long {
        val post = postRepository.findByIdOrNull(id)
            ?: throw IllegalArgumentException("존재하지 않는 게시글입니다.")

        // 1. 이미지 정리: (기존 본문 이미지) - (새 본문 이미지) = 삭제 대상
        val oldImages = extractImageNamesFromContent(post.content)
        val newImages = extractImageNamesFromContent(request.content)
        val removedImages = oldImages - newImages.toSet()

        removedImages.forEach { imageService.deleteImage(it) }

        // 2. 카테고리 조회
        val category = request.categoryId?.let { categoryRepository.findByIdOrNull(it) }

        // 3. Slug 갱신 (변경 요청이 있고, 기존과 다를 경우에만)
        var newSlug = post.slug
        if (!request.slug.isNullOrBlank() && request.slug != post.slug) {
            newSlug = generateUniqueSlug(request.slug, request.title)
        }

        // 4. 정보 업데이트
        post.update(request.title, request.content, newSlug, category)

        // 5. 태그 업데이트
        val newPostTags = resolveTags(request.tags, post)
        post.updateTags(newPostTags)

        return post.id!!
    }

    @Transactional
    fun deletePost(id: Long) {
        val post = postRepository.findByIdOrNull(id)
            ?: throw IllegalArgumentException("존재하지 않는 게시글입니다.")

        val imageNames = extractImageNamesFromContent(post.content)
        imageNames.forEach { fileName ->
            imageService.deleteImage(fileName)
        }

        postRepository.delete(post)
    }

    fun searchPosts(keyword: String?, categoryName: String?, tagName: String?, pageable: Pageable): Page<PostSummaryResponse> {
        val categoryNames = if (categoryName != null) {
            getCategoryAndDescendants(categoryName)
        } else {
            null
        }

        return postRepository.search(keyword, categoryNames, tagName, pageable)
    }

    // --- Helper Methods ---

    // Slug 중복 처리 로직 분리
    private fun generateUniqueSlug(inputSlug: String?, title: String): String {
        val rawSlug = if (!inputSlug.isNullOrBlank()) {
            inputSlug
        } else {
            title.trim().replace("\\s+".toRegex(), "-").lowercase()
        }

        var uniqueSlug = rawSlug
        var count = 1

        uniqueSlug = uniqueSlug.replace("?", "")
        uniqueSlug = uniqueSlug.replace(";", "")

        while (postRepository.existsBySlug(uniqueSlug)) {
            uniqueSlug = "$rawSlug-$count"
            count++
        }
        return uniqueSlug
    }

    // 태그 이름 -> PostTag 변환 로직 분리
    private fun resolveTags(tagNames: List<String>, post: Post): List<PostTag> {
        return tagNames.map { tagName ->
            val tag = tagRepository.findByName(tagName)
                ?: tagRepository.save(Tag(name = tagName))
            PostTag(post = post, tag = tag)
        }
    }

    private fun extractImageNamesFromContent(content: String): List<String> {
        val regex = Regex("!\\[.*?\\]\\((.*?)\\)")
        return regex.findAll(content)
            .map { it.groupValues[1] }
            .map { it.substringAfterLast("/") }
            .toList()
    }

    private fun getCategoryAndDescendants(categoryName: String): List<String> {
        if (categoryName.equals("uncategorized", ignoreCase = true)) {
            return listOf("uncategorized")
        }

        val category = categoryRepository.findByName(categoryName)
        if (category == null) return listOf(categoryName)

        val names = mutableListOf<String>()
        collectCategoryNames(category, names)
        return names
    }

    private fun collectCategoryNames(category: Category, names: MutableList<String>) {
        names.add(category.name)
        category.children.forEach { collectCategoryNames(it, names) }
    }
}