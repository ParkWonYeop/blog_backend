package me.wypark.blogbackend.domain.user

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import me.wypark.blogbackend.domain.common.BaseTimeEntity

@Entity
@Table(name = "member")
class Member(
    @Column(nullable = false, unique = true)
    val email: String,

    @Column(nullable = false)
    val password: String,

    @Column(nullable = false)
    val nickname: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: Role,

    isVerified: Boolean = false
) : BaseTimeEntity() {

    @Column(nullable = false)
    var isVerified: Boolean = isVerified
        protected set

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    fun verify() {
        this.isVerified = true
    }
}

enum class Role {
    ROLE_USER,
    ROLE_ADMIN
}
