package me.wypark.blogbackend.domain.mcp.repository

import me.wypark.blogbackend.domain.mcp.entity.McpKey
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository

interface McpKeyRepository : JpaRepository<McpKey, Long> {
    @EntityGraph(attributePaths = ["member"])
    fun findByTokenHash(tokenHash: String): McpKey?

    fun findAllByMemberIdOrderByCreatedAtDescIdDesc(memberId: Long): List<McpKey>
    fun findByIdAndMemberId(id: Long, memberId: Long): McpKey?
}
