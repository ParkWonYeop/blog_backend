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
import java.time.LocalDate
import java.time.ZoneId

/**
 * [게시글 비즈니스 로직]
 *
 * 게시글(Post)의 생명주기(Lifecycle) 전반을 관리하는 서비스입니다.
 * 단순 CRUD 외에도 다음과 같은 중요한 정책들을 수행합니다.
 *
 * 1. 리소스 정리: 게시글 수정/삭제 시 본문에서 제외된 이미지를 S3에서 물리적으로 삭제하여 스토리지 비용을 최적화합니다.
 * 2. URL 전략: 검색 엔진 최적화(SEO)를 위해 중복되지 않는 고유한 Slug를 생성하고 관리합니다.
 * 3. 검색 확장: 카테고리 검색 시 하위 카테고리의 글까지 포함하여 조회하는 재귀적 검색 로직을 제공합니다.
 */
@Service
@Transactional(readOnly = true)
class PostService(
    private val postRepository: PostRepository,
    private val categoryRepository: CategoryRepository,
    private val memberRepository: MemberRepository,
    private val tagRepository: TagRepository,
    private val imageService: ImageService,
    private val postViewDailyStatsJdbcRepository: PostViewDailyStatsJdbcRepository
) {

    /**
     * 전체 게시글 목록을 조회합니다.
     * 목록 뷰에서는 본문 전체가 필요 없으므로, 경량화된 DTO(Summary)로 변환하여 트래픽을 절감합니다.
     */
    fun getPosts(pageable: Pageable): Page<PostSummaryResponse> {
        return postRepository.findAll(pageable)
            .map { PostSummaryResponse.from(it) }
    }

    /**
     * 게시글 상세 정보를 조회합니다.
     *
     * [부가 로직]
     * 1. 조회수 증가: 상세 조회 시 조회수 카운트를 원자적(Atomic)으로 증가시킵니다.
     * 2. 인접 게시글 탐색: 사용자의 탐색 연속성(UX)을 위해 현재 글을 기준으로 이전/다음 글의 메타데이터를 함께 반환합니다.
     */
    @Transactional
    fun getPostBySlug(slug: String): PostResponse {
        val post = postRepository.findBySlug(slug)
            ?: throw IllegalArgumentException("해당 게시글을 찾을 수 없습니다: $slug")

        post.increaseViewCount()
        postViewDailyStatsJdbcRepository.incrementPostView(post.id!!, LocalDate.now(KOREA_ZONE_ID))

        // 인접 게시글 조회 (Prev/Next Navigation)
        // ID를 기준으로 정렬하여 바로 앞/뒤의 게시글을 1건씩 조회합니다.
        val prevPost = postRepository.findFirstByIdLessThanOrderByIdDesc(post.id!!)
        val nextPost = postRepository.findFirstByIdGreaterThanOrderByIdAsc(post.id!!)

        return PostResponse.from(post, prevPost, nextPost)
    }

    /**
     * 신규 게시글을 생성합니다.
     *
     * [Slug 생성 전략]
     * 사용자가 Slug를 직접 입력하지 않은 경우 제목을 기반으로 생성하며,
     * 중복 발생 시 숫자를 붙여(suffix) 유일성을 보장하는 재귀적/반복적 로직을 수행합니다.
     */
    @Transactional
    fun createPost(request: PostSaveRequest, email: String): Long {
        val member = memberRepository.findByEmail(email)
            ?: throw IllegalArgumentException("회원 없음")

        val category = request.categoryId?.let { categoryRepository.findByIdOrNull(it) }

        // SEO Friendly URL 생성을 위한 Slug 중복 검사 및 생성
        val uniqueSlug = generateUniqueSlug(request.slug, request.title)

        val post = Post(
            title = request.title,
            content = request.content,
            slug = uniqueSlug,
            member = member,
            category = category
        )

        // 태그 처리: 기존 태그는 재사용, 없는 태그는 신규 생성 (Find or Create)
        val postTags = resolveTags(request.tags, post)
        post.addTags(postTags)

        return postRepository.save(post).id!!
    }

    /**
     * 게시글 정보를 수정합니다.
     *
     * [이미지 가비지 컬렉션 (GC)]
     * 본문 수정 과정에서 삭제된 이미지 태그를 감지하여, 실제 스토리지(S3)에서도 파일을 삭제합니다.
     * 이를 통해 DB와 스토리지 간의 데이터 불일치를 방지하고 불필요한 비용 발생을 억제합니다.
     */
    @Transactional
    fun updatePost(id: Long, request: PostSaveRequest): Long {
        val post = postRepository.findByIdOrNull(id)
            ?: throw IllegalArgumentException("존재하지 않는 게시글입니다.")

        // 1. 고아 이미지 정리: (수정 전 이미지 목록 - 수정 후 이미지 목록)
        val oldImages = extractImageNamesFromContent(post.content)
        val newImages = extractImageNamesFromContent(request.content)
        val removedImages = oldImages - newImages.toSet()

        removedImages.forEach { imageService.deleteImage(it) }

        // 2. 카테고리 정보 갱신
        val category = request.categoryId?.let { categoryRepository.findByIdOrNull(it) }

        // 3. Slug 갱신 (변경 요청 시에만 수행하여 불필요한 URL 변경 방지)
        var newSlug = post.slug
        if (!request.slug.isNullOrBlank() && request.slug != post.slug) {
            newSlug = generateUniqueSlug(request.slug, request.title)
        }

        // 4. 게시글 메타데이터 업데이트 (Dirty Checking)
        post.update(request.title, request.content, newSlug, category)

        // 5. 태그 매핑 재설정
        val newPostTags = resolveTags(request.tags, post)
        post.updateTags(newPostTags)

        return post.id!!
    }

    /**
     * 게시글을 삭제합니다.
     *
     * [Cascading Deletion]
     * 게시글 엔티티뿐만 아니라, 본문에 포함된 모든 이미지 파일도 스토리지에서 제거합니다.
     * 태그 매핑 정보 등은 JPA Cascade 설정에 의해 자동으로 정리됩니다.
     */
    @Transactional
    fun deletePost(id: Long) {
        val post = postRepository.findByIdOrNull(id)
            ?: throw IllegalArgumentException("존재하지 않는 게시글입니다.")

        // 본문에 포함된 이미지 추출 및 삭제
        val imageNames = extractImageNamesFromContent(post.content)
        imageNames.forEach { fileName ->
            imageService.deleteImage(fileName)
        }

        postRepository.delete(post)
    }

    /**
     * 복합 조건 검색을 수행합니다.
     *
     * [계층형 카테고리 검색]
     * 상위 카테고리로 검색 시, 해당 카테고리에 속한 하위 카테고리(Descendants)의 게시글들도
     * 모두 결과에 포함되도록 검색 조건을 확장(Expand)합니다.
     */
    fun searchPosts(keyword: String?, categoryName: String?, tagName: String?, pageable: Pageable): Page<PostSummaryResponse> {
        val categoryNames = if (categoryName != null) {
            getCategoryAndDescendants(categoryName)
        } else {
            null
        }

        return postRepository.search(keyword, categoryNames, tagName, pageable)
    }

    // --- Helper Methods ---

    /**
     * Slug 중복 발생 시, 카운팅 숫자를 접미사(Suffix)로 붙여 유일한 값을 생성합니다.
     * 예: "hello-world" -> "hello-world-1" -> "hello-world-2"
     */
    private fun generateUniqueSlug(inputSlug: String?, title: String): String {
        val rawSlug = if (!inputSlug.isNullOrBlank()) {
            inputSlug
        } else {
            // URL에 안전하지 않은 문자 제거 및 공백 치환
            title.trim().replace("\\s+".toRegex(), "-").lowercase()
        }

        var uniqueSlug = rawSlug
        var count = 1

        // 특수문자 정제
        uniqueSlug = uniqueSlug.replace("?", "")
        uniqueSlug = uniqueSlug.replace(";", "")

        // 중복 체크 루프
        while (postRepository.existsBySlug(uniqueSlug)) {
            uniqueSlug = "$rawSlug-$count"
            count++
        }
        return uniqueSlug
    }

    /**
     * 태그 문자열 리스트를 PostTag 엔티티 리스트로 변환합니다.
     * DB에 존재하지 않는 태그는 즉시 생성(Save)하여 매핑합니다.
     */
    private fun resolveTags(tagNames: List<String>, post: Post): List<PostTag> {
        return tagNames.map { tagName ->
            val tag = tagRepository.findByName(tagName)
                ?: tagRepository.save(Tag(name = tagName))
            PostTag(post = post, tag = tag)
        }
    }

    /**
     * 정규표현식을 사용하여 Markdown 본문에서 이미지 URL(파일명)을 추출합니다.
     * 패턴: ![alt](url)
     */
    private fun extractImageNamesFromContent(content: String): List<String> {
        val regex = Regex("!\\[.*?\\]\\((.*?)\\)")
        return regex.findAll(content)
            .map { it.groupValues[1] }
            .map { it.substringAfterLast("/") }
            .toList()
    }

    /**
     * 특정 카테고리의 모든 자손 카테고리 이름을 재귀적으로 수집합니다.
     * "Parent" 검색 시 "Parent > Child"의 글도 나오게 하기 위함입니다.
     */
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

    companion object {
        private val KOREA_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
