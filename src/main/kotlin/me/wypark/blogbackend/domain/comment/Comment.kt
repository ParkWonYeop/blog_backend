package me.wypark.blogbackend.domain.comment

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
import me.wypark.blogbackend.domain.common.BaseTimeEntity
import me.wypark.blogbackend.domain.post.Post
import me.wypark.blogbackend.domain.user.Member

@Entity
class Comment(
    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    val post: Post,

    parent: Comment? = null,

    @OneToMany(mappedBy = "parent", cascade = [CascadeType.ALL], orphanRemoval = true)
    val children: MutableList<Comment> = mutableListOf(),


