package me.wypark.blogbackend.domain.user

import jakarta.persistence.*
import me.wypark.blogbackend.domain.common.BaseTimeEntity

@Entity
@Table(name = "member")
class Member(
    @Column(nullable = false, unique = true)
    val email: String,

    @Column(nullable = false)
    var password: String,

    @Column(nullable = false)
    var nickname: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: Role,

    @Column(nullable = false)
  var isVerified: Boolean = false
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    fun verify() {
        this.isVerified = true
    }
}

enum class Role {
    ROLE_USER, ROLE_ADMIN
}