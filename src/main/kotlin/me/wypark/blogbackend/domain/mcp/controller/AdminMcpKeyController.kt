package me.wypark.blogbackend.domain.mcp.controller

import jakarta.validation.Valid
import me.wypark.blogbackend.domain.mcp.dto.McpKeyCreateRequest
import me.wypark.blogbackend.domain.mcp.service.McpKeyService
import me.wypark.blogbackend.global.common.ApiResponse
import org.springframework.http.CacheControl
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.User
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/mcp/keys")
class AdminMcpKeyController(private val keys: McpKeyService) {
    @GetMapping
    fun list(@AuthenticationPrincipal user: User) = ResponseEntity.ok()
        .cacheControl(CacheControl.noStore()).body(ApiResponse.success(keys.list(user.username)))

    @PostMapping
    fun create(@AuthenticationPrincipal user: User, @RequestBody @Valid request: McpKeyCreateRequest) =
        ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(ApiResponse.success(keys.create(user.username, request), "연결 키가 발급되었습니다."))

    @DeleteMapping("/{id}")
    fun revoke(@AuthenticationPrincipal user: User, @PathVariable id: Long): ApiResponse<Nothing> {
        keys.revoke(user.username, id)
        return ApiResponse.success(message = "연결 키가 폐기되었습니다.")
    }
}
