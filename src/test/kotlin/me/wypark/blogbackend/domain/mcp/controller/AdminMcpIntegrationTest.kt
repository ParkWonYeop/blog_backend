package me.wypark.blogbackend.domain.mcp.controller

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.modelcontextprotocol.client.McpClient
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport
import io.modelcontextprotocol.spec.McpSchema
import me.wypark.blogbackend.domain.mcp.dto.McpKeyCreateRequest
import me.wypark.blogbackend.domain.mcp.dto.McpKeyCreatedResponse
import me.wypark.blogbackend.domain.mcp.entity.McpPermission
import me.wypark.blogbackend.domain.mcp.repository.McpKeyRepository
import me.wypark.blogbackend.domain.mcp.service.McpKeyService
import me.wypark.blogbackend.domain.post.repository.PostRepository
import me.wypark.blogbackend.domain.user.entity.Member
import me.wypark.blogbackend.domain.user.entity.Role
import me.wypark.blogbackend.domain.user.repository.MemberRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminMcpIntegrationTest {
    @Autowired lateinit var mvc: MockMvc
    @Autowired lateinit var mapper: ObjectMapper
    @Autowired lateinit var members: MemberRepository
    @Autowired lateinit var keys: McpKeyRepository
    @Autowired lateinit var keyService: McpKeyService
    @Autowired lateinit var posts: PostRepository
    @Autowired lateinit var jdbc: JdbcTemplate
    @LocalServerPort var port: Int = 0

    @Test
    fun `keys require an administrator and creation validates input`() {
        mvc.perform(get(KEYS)).andExpect(status().isUnauthorized)
        mvc.perform(post(KEYS).with(user("member").roles("USER"))
            .contentType(MediaType.APPLICATION_JSON).content("""{"name":"client"}"""))
            .andExpect(status().isForbidden)
        val admin = admin()
        for (body in listOf("""{"name":" "}""", """{"name":"client","expiresInDays":0}""",
            """{"name":"client","expiresInDays":366}""", """{"name":"client","permission":"ADMIN"}""")) {
            mvc.perform(post(KEYS).with(user(admin.email).roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest)
        }
    }

    @Test
    fun `secret is returned once hashed in storage and only its owner may revoke`() {
        val admin = admin()
        val issued = mvc.perform(post(KEYS).with(user(admin.email).roles("ADMIN"))
            .contentType(MediaType.APPLICATION_JSON).content("""{"name":"Codex"}"""))
            .andExpect(status().isOk).andExpect(header().string("Cache-Control", "no-store"))
            .andReturn().response.contentAsString.let(mapper::readTree)["data"]
        val id = issued["key"]["id"].asLong()
        val token = issued["token"].asText()
        val stored = keys.findById(id).orElseThrow()
        assertTrue(token.matches(Regex("bmcp_[A-Za-z0-9_-]{43}")))
        assertEquals(64, stored.tokenHash.length)
        assertNotEquals(token, stored.tokenHash)
        assertEquals(McpPermission.READ_ONLY, stored.permission)
        assertEquals(90, ChronoUnit.DAYS.between(stored.createdAt, stored.expiresAt))
        mvc.perform(get(KEYS).with(user(admin.email).roles("ADMIN")))
            .andExpect(status().isOk).andExpect(jsonPath("$.data[0].id").value(id))
            .andExpect(jsonPath("$.data[0].token").doesNotExist())
            .andExpect(jsonPath("$.data[0].tokenHash").doesNotExist())
        val other = admin()
        mvc.perform(get(KEYS).with(user(other.email).roles("ADMIN")))
            .andExpect(jsonPath("$.data.length()").value(0))
        mvc.perform(delete("$KEYS/$id").with(user(other.email).roles("ADMIN")))
            .andExpect(status().isNotFound)
        mvc.perform(delete("$KEYS/$id").with(user(admin.email).roles("ADMIN")))
            .andExpect(status().isOk)
        mcp(token, "tools/list").andExpect(status().isUnauthorized)
    }

    @Test
    fun `MCP key cannot authenticate REST management and JWT cannot substitute for an MCP key`() {
        val token = issue().token
        mvc.perform(get(KEYS).header("Authorization", "Bearer $token")).andExpect(status().isUnauthorized)
        mvc.perform(post("/api/admin/posts").header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnauthorized)
        mvc.perform(post(MCP).with(user("admin").roles("ADMIN"))
            .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isForbidden)
        mcp("invalid", "tools/list").andExpect(status().isUnauthorized)
        mvc.perform(post(MCP).param("token", token).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `expired revoked and demoted owner keys stop authenticating immediately`() {
        val expired = issue()
        jdbc.update("UPDATE mcp_key SET expires_at = ? WHERE id = ?", Instant.now().minusSeconds(1), expired.key.id)
        mcp(expired.token, "tools/list").andExpect(status().isUnauthorized)
        val admin = admin()
        val demoted = keyService.create(admin.email, McpKeyCreateRequest("demoted"))
        jdbc.update("UPDATE member SET role = 'ROLE_USER' WHERE id = ?", admin.id)
        mcp(demoted.token, "tools/list").andExpect(status().isUnauthorized)
        val unverified = admin()
        val key = keyService.create(unverified.email, McpKeyCreateRequest("unverified"))
        jdbc.update("UPDATE member SET is_verified = false WHERE id = ?", unverified.id)
        mcp(key.token, "tools/list").andExpect(status().isUnauthorized)
    }

    @Test
    fun `standard HTTP lifecycle advertises tools and rejects foreign origins`() {
        val token = issue().token
        mcp(token, "initialize", mapOf("protocolVersion" to "2025-11-25", "capabilities" to emptyMap<String, Any>(),
            "clientInfo" to mapOf("name" to "test", "version" to "1")))
            .andExpect(status().isOk).andExpect(jsonPath("$.result.serverInfo.name").value("wypark-blog-admin"))
            .andExpect(jsonPath("$.result.capabilities.tools").exists())
        mvc.perform(post(MCP).header("Authorization", "Bearer $token").accept(ACCEPT)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"jsonrpc":"2.0","method":"notifications/initialized"}"""))
            .andExpect(status().isAccepted)
        mcp(token, "tools/list").andExpect(status().isOk)
            .andExpect(jsonPath("$.result.tools.length()").value(14))
        mvc.perform(get(MCP).header("Authorization", "Bearer $token")).andExpect(status().isMethodNotAllowed)
        mvc.perform(post(MCP).header("Authorization", "Bearer $token").header("Origin", "https://evil.example")
            .contentType(MediaType.APPLICATION_JSON).accept(ACCEPT).content("{}"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `read only key rejects every mutating tool`() {
        val token = issue().token
        val mutating = listOf("create_post", "update_post", "delete_post", "create_category", "update_category",
            "delete_category", "create_comment", "delete_comment", "update_profile")
        for (name in mutating) {
            val result = call(token, "blog_$name")
            assertTrue(result["isError"].asBoolean(), name)
            assertTrue(result["content"][0]["text"].asText().contains("읽기 전용"))
        }
    }

    @Test
    fun `writes use existing services and reads preserve editing fields without counting views`() {
        val token = issue(McpPermission.READ_WRITE).token
        val categoryId = success(call(token, "blog_create_category", mapOf("category" to
            mapOf("name" to "MCP-${UUID.randomUUID()}", "parentId" to null))))["id"].asLong()
        val post = mapOf("title" to "MCP test", "content" to "# Markdown", "categoryId" to categoryId,
            "tags" to listOf("mcp", "kotlin"))
        val postId = success(call(token, "blog_create_post", mapOf("post" to post)))["id"].asLong()
        val detail = success(call(token, "blog_get_post", mapOf("id" to postId)))
        assertEquals(categoryId, detail["categoryId"].asLong())
        assertEquals(2, detail["tags"].size())
        assertEquals(0, posts.findById(postId).orElseThrow().viewCount)
        success(call(token, "blog_update_post", mapOf("id" to postId, "post" to (post + ("content" to "Updated")))))
        assertEquals("Updated", posts.findById(postId).orElseThrow().content)
        val commentId = success(call(token, "blog_create_comment", mapOf("comment" to
            mapOf("postSlug" to detail["slug"].asText(), "content" to "Admin reply"))))["id"].asLong()
        success(call(token, "blog_list_comments"))
        success(call(token, "blog_delete_comment", mapOf("id" to commentId, "confirm" to true)))
        for (confirm in listOf(false, "true", null)) {
            assertTrue(call(token, "blog_delete_post", mapOf("id" to postId, "confirm" to confirm))["isError"].asBoolean())
            assertTrue(posts.existsById(postId))
        }
        success(call(token, "blog_delete_post", mapOf("id" to postId, "confirm" to true)))
        assertFalse(posts.existsById(postId))
        success(call(token, "blog_delete_category", mapOf("id" to categoryId, "confirm" to true)))
    }

    @Test
    fun `schema and bean validation block malformed and unbounded requests`() {
        val token = issue(McpPermission.READ_WRITE).token
        for (args in listOf(mapOf("size" to 101), mapOf("page" to -1), mapOf("page" to 0.5),
            mapOf("size" to "20"), mapOf("unknown" to true))) {
            assertTrue(call(token, "blog_list_posts", args)["isError"].asBoolean())
        }
        assertTrue(call(token, "blog_create_post", mapOf("post" to mapOf(
            "title" to " ", "content" to "body", "categoryId" to null, "tags" to emptyList<String>()
        )))["isError"].asBoolean())
        assertTrue(call(token, "blog_get_post", mapOf("id" to Long.MAX_VALUE))["isError"].asBoolean())
        assertTrue(call(token, "blog_create_post", mapOf("post" to mapOf(
            "title" to "Title", "content" to "body", "categoryId" to null, "tags" to listOf(" ")
        )))["isError"].asBoolean())
        mcp(token, "tools/call", mapOf("name" to "missing", "arguments" to emptyMap<String, Any>()))
            .andExpect(status().isOk).andExpect(jsonPath("$.error.code").value(-32602))
    }

    @Test
    fun `official SDK client connects over HTTP and reads tools`() {
        val token = issue().token
        val transport = HttpClientStreamableHttpTransport.builder("http://localhost:$port")
            .endpoint(MCP).customizeRequest { it.header("Authorization", "Bearer $token") }.build()
        McpClient.sync(transport).requestTimeout(Duration.ofSeconds(10)).build().use { client ->
            assertEquals("wypark-blog-admin", client.initialize().serverInfo().name())
            assertEquals(14, client.listTools().tools().size)
            val result = client.callTool(McpSchema.CallToolRequest("blog_list_posts", mapOf("size" to 1)))
            assertEquals(false, result.isError())
        }
    }

    private fun admin(): Member = members.save(Member(
        "mcp-${UUID.randomUUID()}@example.com", "encoded", "mcp-${UUID.randomUUID()}", Role.ROLE_ADMIN, true
    ))

    private fun issue(permission: McpPermission = McpPermission.READ_ONLY): McpKeyCreatedResponse =
        keyService.create(admin().email, McpKeyCreateRequest("test-client", permission))

    private fun mcp(token: String, method: String, params: Map<String, Any?> = emptyMap()) = mvc.perform(
        post(MCP).header("Authorization", "Bearer $token").accept(ACCEPT)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(mapOf("jsonrpc" to "2.0", "id" to 1, "method" to method, "params" to params)))
    )

    private fun call(token: String, name: String, args: Map<String, Any?> = emptyMap()): JsonNode =
        mcp(token, "tools/call", mapOf("name" to name, "arguments" to args)).andExpect(status().isOk)
            .andReturn().response.contentAsString.let(mapper::readTree)["result"]

    private fun success(result: JsonNode): JsonNode {
        assertFalse(result["isError"].asBoolean(), result.toString())
        return result["structuredContent"]
    }

    companion object {
        private const val MCP = "/api/admin/mcp"
        private const val KEYS = "$MCP/keys"
        private const val ACCEPT = "application/json, text/event-stream"
    }
}
