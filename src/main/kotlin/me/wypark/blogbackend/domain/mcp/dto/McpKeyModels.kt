package me.wypark.blogbackend.domain.mcp.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import me.wypark.blogbackend.domain.mcp.entity.McpKey
import me.wypark.blogbackend.domain.mcp.entity.McpPermission
import java.time.Instant

data class McpKeyCreateRequest(
    @field:NotBlank
    @field:Size(max = 100)
    val name: String,
    val permission: McpPermission = McpPermission.READ_ONLY,
    @field:Min(1)
    @field:Max(365)
    val expiresInDays: Int = 90
)

data class McpKeyResponse(
    val id: Long,
    val name: String,
    val tokenPrefix: String,
    val permission: McpPermission,
    val createdAt: Instant,
    val expiresAt: Instant,
    val revokedAt: Instant?
) {
    companion object {
        fun from(key: McpKey) = McpKeyResponse(
            requireNotNull(key.id), key.name, key.tokenPrefix, key.permission,
            key.createdAt, key.expiresAt, key.revokedAt
        )
    }
}

// Do not make this a data class: its generated toString would expose the credential.
class McpKeyCreatedResponse(val key: McpKeyResponse, val token: String)

data class McpIdentity(val keyId: Long, val email: String, val permission: McpPermission)
