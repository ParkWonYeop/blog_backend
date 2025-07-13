package me.wypark.blogbackend.domain.profile

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import me.wypark.blogbackend.domain.common.BaseTimeEntity

@Entity
@Table(name = "blog_profile")
class BlogProfile(
    name: String,
    bio: String,
    imageUrl: String? = null,
    githubUrl: String? = null,
    email: String? = null
) : BaseTimeEntity() {

    @Column(nullable = false)
    var name: String = name
        protected set

    @Column(columnDefinition = "TEXT")
    var bio: String = bio
        protected set

    @Column
    var imageUrl: String? = imageUrl
        protected set

    @Column
    var githubUrl: String? = githubUrl
        protected set

    @Column
    var email: String? = email
        protected set

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
