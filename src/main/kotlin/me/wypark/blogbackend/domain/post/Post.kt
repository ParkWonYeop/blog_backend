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

