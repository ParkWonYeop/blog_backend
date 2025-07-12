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

