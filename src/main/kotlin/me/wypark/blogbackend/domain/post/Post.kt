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

    @Column(columnDefinition = "TEXT", nullable = false)
    var content: String,

    @Column(nullable = false, unique = true)
    var slug: String,

    @Column(nullable = false)
    var viewCount: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    val member: Member,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    var category: Category? = null,

    @OneToMany(mappedBy = "post", cascade = [CascadeType.ALL], orphanRemoval = true)
    val tags: MutableList<PostTag> = mutableListOf()
) : BaseTimeEntity() {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    fun increaseViewCount() {
        this.viewCount++
    }

    fun addTags(postTags: List<PostTag>) {
        this.tags.addAll(postTags)
    }

    // 👈 [추가] 게시글 수정 메서드
    fun update(title: String, content: String, slug: String, category: Category?) {
        this.title = title
        this.content = content
        this.slug = slug
        this.category = category
    }

    // 👈 [추가] 태그 전체 교체 편의 메서드
    fun updateTags(newTags: List<PostTag>) {
        this.tags.clear() // orphanRemoval = true 덕분에 기존 태그 매핑이 삭제됨
        this.tags.addAll(newTags)
    }
}