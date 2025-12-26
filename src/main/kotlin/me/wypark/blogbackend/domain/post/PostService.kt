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
        if (postRepository.existsBySlug(request.slug)) { throw IllegalArgumentException("이미 존재하는 Slug입니다.") }
        val member = memberRepository.findByEmail(email) ?: throw IllegalArgumentException("회원 없음")
        val category = request.categoryId?.let { categoryRepository.findByIdOrNull(it) }

        val post = Post(
            title = request.title,
            content = request.content,
            slug = request.slug,
            member = member,
            category = category
        )

        val postTags = request.tags.map { tagName ->
            val tag = tagRepository.findByName(tagName)
                ?: tagRepository.save(Tag(name = tagName))

            PostTag(post = post, tag = tag)
        }

        post.addTags(postTags)

        return postRepository.save(post).id!!
    }

    fun searchPosts(keyword: String?, categoryName: String?, tagName: String?, pageable: Pageable): Page<PostSummaryResponse> {
        return postRepository.search(keyword, categoryName, tagName, pageable)
    }
}