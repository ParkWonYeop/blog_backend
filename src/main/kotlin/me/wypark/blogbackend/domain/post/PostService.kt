package me.wypark.blogbackend.domain.post

import me.wypark.blogbackend.api.dto.PostResponse
import me.wypark.blogbackend.api.dto.PostSaveRequest
import me.wypark.blogbackend.api.dto.PostSummaryResponse
import me.wypark.blogbackend.domain.category.CategoryRepository
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
    private val tagRepository: TagRepository
) {

    /**
     * [Public] 전체 게시글 목록 조회 (페이징)
     */
    fun getPosts(pageable: Pageable): Page<PostSummaryResponse> {
        return postRepository.findAll(pageable)
            .map { PostSummaryResponse.from(it) }
    }

    /**
     * [Public] 게시글 상세 조회 (Slug 기반) + 조회수 증가
     */
    @Transactional
    fun getPostBySlug(slug: String): PostResponse {
        val post = postRepository.findBySlug(slug)
            ?: throw IllegalArgumentException("해당 게시글을 찾을 수 없습니다: $slug")

        post.increaseViewCount() // 조회수 1 증가 (Dirty Checking)

        return PostResponse.from(post)
    }

    /**
     * [Admin] 게시글 작성
     */
    @Transactional
    fun createPost(request: PostSaveRequest, email: String): Long {
        val member = memberRepository.findByEmail(email)
            ?: throw IllegalArgumentException("회원 없음")

        val category = request.categoryId?.let { categoryRepository.findByIdOrNull(it) }

        val rawSlug = if (!request.slug.isNullOrBlank()) {
            request.slug
        } else {
            request.title.trim().replace("\\s+".toRegex(), "-").lowercase()
        }

        // (2) DB 중복 검사: 중복되면 -1, -2, -3... 붙여나감
        var uniqueSlug = rawSlug
        var count = 1

        while (postRepository.existsBySlug(uniqueSlug)) {
            uniqueSlug = "$rawSlug-$count"
            count++
        }
        // ---------------------------------------------------------

        // 2. 게시글 객체 생성 (uniqueSlug 사용)
        val post = Post(
            title = request.title,
            content = request.content,
            slug = uniqueSlug, // 👈 중복 처리된 슬러그
            member = member,
            category = category
        )

        // 3. 태그 처리 (작성하신 로직 그대로 활용)
        // 리스트를 순회하며 없으면 저장(save), 있으면 조회(find)
        val postTags = request.tags.map { tagName ->
            val tag = tagRepository.findByName(tagName)
                ?: tagRepository.save(Tag(name = tagName))

            PostTag(post = post, tag = tag)
        }

        // 연관관계 편의 메서드 사용 (Post 내부에 구현되어 있다고 가정)
        post.addTags(postTags)

        return postRepository.save(post).id!!
    }

    fun searchPosts(keyword: String?, categoryName: String?, tagName: String?, pageable: Pageable): Page<PostSummaryResponse> {
        return postRepository.search(keyword, categoryName, tagName, pageable)
    }
}