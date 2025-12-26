package me.wypark.blogbackend.domain.post

import jakarta.persistence.*
import me.wypark.blogbackend.domain.category.Category
import me.wypark.blogbackend.domain.common.BaseTimeEntity
import me.wypark.blogbackend.domain.tag.PostTag
import me.wypark.blogbackend.domain.user.Member

@Entity
class Post(
    @Column(nullable = false)
    var title: String,

    // 마크다운 본문 (대용량 저장을 위해 TEXT 타입 지정)
    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,

    @Column(nullable = false, unique = true)
    var slug: String, // URL용 제목 (예: my-first-post)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    val member: Member, // 작성자 (관리자)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    var category: Category? = null // 카테고리 (없을 수도 있음)

) : BaseTimeEntity() { // 생성일, 수정일 자동 관리

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @Column(nullable = false)
    var viewCount: Long = 0

    // 조회수 증가
    fun increaseViewCount() {
        this.viewCount++
    }

    // 게시글 수정
    fun update(title: String, content: String, slug: String, category: Category?) {
        this.title = title
        this.content = content
        this.slug = slug
        this.category = category
    }

    @OneToMany(mappedBy = "post", cascade = [CascadeType.ALL], orphanRemoval = true)
    var tags: MutableList<PostTag> = mutableListOf()

    fun addTags(newTags: List<PostTag>) {
        this.tags.clear()
        this.tags.addAll(newTags)
    }
}