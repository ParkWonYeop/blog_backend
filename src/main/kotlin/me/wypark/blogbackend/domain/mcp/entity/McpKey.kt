package me.wypark.blogbackend.domain.mcp.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import me.wypark.blogbackend.domain.user.entity.Member
import java.time.Instant

enum class McpPermission { READ_ONLY, READ_WRITE }

@Entity
@Table(name = "mcp_key")
class McpKey(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    val member: Member,
    @Column(nullable = false, length = 100)
    val name: String,
    @Column(nullable = false, unique = true, length = 64)
    val tokenHash: String,
    @Column(nullable = false, length = 13)
    val tokenPrefix: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val permission: McpPermission,
    @Column(nullable = false)
    val createdAt: Instant,
    @Column(nullable = false)
    val expiresAt: Instant
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    var revokedAt: Instant? = null
        protected set

    fun isActive(now: Instant): Boolean = revokedAt == null && expiresAt.isAfter(now)

    fun revoke(now: Instant) {
        if (revokedAt == null) revokedAt = now
    }
}
