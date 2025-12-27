package me.wypark.blogbackend.domain.profile

import jakarta.persistence.*
import me.wypark.blogbackend.domain.common.BaseTimeEntity

@Entity
@Table(name = "blog_profile")
class BlogProfile(
    @Column(nullable = false)
    var name: String,

    @Column(columnDefinition = "TEXT")
    var bio: String,

    @Column
    var imageUrl: String? = null,

    @Column
    var githubUrl: String? = null,

    @Column
    var email: String? = null
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    fun update(name: String, bio: String, imageUrl: String?, githubUrl: String?, email: String?) {
        this.name = name
        this.bio = bio
        this.imageUrl = imageUrl
        this.githubUrl = githubUrl
        this.email = email
    }
}