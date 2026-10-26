package me.wypark.blogbackend.domain.mcp.service

import me.wypark.blogbackend.domain.mcp.dto.McpIdentity
import me.wypark.blogbackend.domain.mcp.dto.McpKeyCreateRequest
import me.wypark.blogbackend.domain.mcp.dto.McpKeyCreatedResponse
import me.wypark.blogbackend.domain.mcp.dto.McpKeyResponse
import me.wypark.blogbackend.domain.mcp.entity.McpKey
import me.wypark.blogbackend.domain.mcp.repository.McpKeyRepository
import me.wypark.blogbackend.domain.user.entity.Member
import me.wypark.blogbackend.domain.user.entity.Role
import me.wypark.blogbackend.domain.user.repository.MemberRepository
import me.wypark.blogbackend.global.common.BusinessException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.time.temporal.ChronoUnit
import java.util.Base64

@Service
@Transactional(readOnly = true)
class McpKeyService(
    private val keys: McpKeyRepository,
    private val members: MemberRepository,
    private val clock: Clock
) {
    fun list(email: String): List<McpKeyResponse> = keys
        .findAllByMemberIdOrderByCreatedAtDescIdDesc(requireNotNull(admin(email).id))
        .map(McpKeyResponse::from)

    @Transactional
    fun create(email: String, request: McpKeyCreateRequest): McpKeyCreatedResponse {
        val member = admin(email)
        val token = TOKEN_PREFIX + Base64.getUrlEncoder().withoutPadding()
            .encodeToString(ByteArray(32).also(random::nextBytes))
        val now = clock.instant()
        val key = keys.save(McpKey(
            member, request.name.trim(), hash(token), token.take(13), request.permission,
            now, now.plus(request.expiresInDays.toLong(), ChronoUnit.DAYS)
        ))
        return McpKeyCreatedResponse(McpKeyResponse.from(key), token)
    }

    @Transactional
    fun revoke(email: String, id: Long) {
        val key = keys.findByIdAndMemberId(id, requireNotNull(admin(email).id))
            ?: throw BusinessException.notFound("연결 키를 찾을 수 없습니다.")
        key.revoke(clock.instant())
    }

    fun authenticate(token: String): McpIdentity? {
        if (!TOKEN_PATTERN.matches(token)) return null
        val key = keys.findByTokenHash(hash(token)) ?: return null
        if (!key.isActive(clock.instant()) || !isAdmin(key.member)) return null
        return McpIdentity(requireNotNull(key.id), key.member.email, key.permission)
    }

    private fun admin(email: String): Member = members.findByEmail(email)?.takeIf(::isAdmin)
        ?: throw BusinessException("관리자 권한이 필요합니다.", "FORBIDDEN", HttpStatus.FORBIDDEN)

    private fun isAdmin(member: Member) = member.role == Role.ROLE_ADMIN && member.isVerified

    private fun hash(token: String): String = MessageDigest.getInstance("SHA-256")
        .digest(token.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

    companion object {
        private const val TOKEN_PREFIX = "bmcp_"
        private val TOKEN_PATTERN = Regex("^bmcp_[A-Za-z0-9_-]{43}$")
        private val random = SecureRandom()
    }
}
