package me.wypark.blogbackend.domain.tag

import jakarta.persistence.*
import me.wypark.blogbackend.domain.post.Post

@Entity
@Table(name = "post_tag")
class PostTag(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    val post: Post,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id")
    val tag: Tag
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
}