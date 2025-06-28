package me.wypark.blogbackend.domain.post

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import me.wypark.blogbackend.domain.category.Category
import me.wypark.blogbackend.domain.common.BaseTimeEntity
import me.wypark.blogbackend.domain.tag.PostTag
import me.wypark.blogbackend.domain.user.Member

@Entity
class Post(
    title: String,
    content: String,
    slug: String,
    viewCount: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    val member: Member,

    category: Category? = null,

    @OneToMany(mappedBy = "post", cascade = [CascadeType.ALL], orphanRemoval = true)
    val tags: MutableList<PostTag> = mutableListOf()
) : BaseTimeEntity() {

    @Column(nullable = false)
    var title: String = title
        protected set

    @Column(columnDefinition = "TEXT", nullable = false)
    var content: String = content
        protected set

    @Column(nullable = false, unique = true)
    var slug: String = slug
        protected set

    @Column(nullable = false)
    var viewCount: Long = viewCount
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    var category: Category? = category
        protected set

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    fun increaseViewCount() {
        this.viewCount++
    }

    fun addTags(postTags: List<PostTag>) {
        this.tags.addAll(postTags)
    }

    fun update(title: String, content: String, slug: String, category: Category?) {
        this.title = title
        this.content = content
        this.slug = slug
        this.category = category
    }

    fun updateTags(newTags: List<PostTag>) {
        this.tags.clear()
        this.tags.addAll(newTags)
    }
}
