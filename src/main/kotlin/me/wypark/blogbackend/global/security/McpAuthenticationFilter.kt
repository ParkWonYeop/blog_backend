package me.wypark.blogbackend.global.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import me.wypark.blogbackend.domain.mcp.service.McpKeyService
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class McpAuthenticationFilter(private val keys: McpKeyService) : OncePerRequestFilter() {
    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        request.requestURI.removePrefix(request.contextPath) != ENDPOINT

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val token = request.getHeader("Authorization")?.takeIf { it.startsWith("Bearer ") }?.substring(7)
        val identity = token?.let(keys::authenticate)
        if (identity != null) {
            request.setAttribute(IDENTITY_ATTRIBUTE, identity)
            SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(
                identity, null, listOf(SimpleGrantedAuthority("ROLE_ADMIN"), SimpleGrantedAuthority("MCP_ACCESS"))
            )
        }
        filterChain.doFilter(request, response)
    }

    companion object {
        const val ENDPOINT = "/api/admin/mcp"
        const val IDENTITY_ATTRIBUTE = "blog.mcp.identity"
    }
}
