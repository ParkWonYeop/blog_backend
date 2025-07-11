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


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    val member: Member? = null,

    @Column
    val guestNickname: String? = null,

    @Column
    val guestPassword: String? = null

) : BaseTimeEntity() {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    var parent: Comment? = parent
        protected set

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    fun getAuthorName(): String {
        return member?.nickname ?: guestNickname ?: "알 수 없음"
    }

    fun addReply(reply: Comment) {
        children.add(reply)
        reply.parent = this
    }

    }
