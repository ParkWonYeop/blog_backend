package me.wypark.blogbackend.domain.mcp.controller

import com.fasterxml.jackson.databind.ObjectMapper
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper
import io.modelcontextprotocol.json.schema.jackson2.DefaultJsonSchemaValidator
import io.modelcontextprotocol.server.McpStatelessServerFeatures
import io.modelcontextprotocol.spec.McpSchema
import jakarta.validation.Validator
import me.wypark.blogbackend.domain.category.dto.CategoryCreateRequest
import me.wypark.blogbackend.domain.category.dto.CategoryUpdateRequest
import me.wypark.blogbackend.domain.category.service.CategoryService
import me.wypark.blogbackend.domain.comment.dto.CommentSaveRequest
import me.wypark.blogbackend.domain.comment.service.CommentService
import me.wypark.blogbackend.domain.mcp.dto.McpIdentity
import me.wypark.blogbackend.domain.mcp.entity.McpPermission
import me.wypark.blogbackend.domain.post.dto.PostSaveRequest
import me.wypark.blogbackend.domain.post.service.PostService
import me.wypark.blogbackend.domain.profile.dto.ProfileUpdateRequest
import me.wypark.blogbackend.domain.profile.service.BlogProfileService
import me.wypark.blogbackend.global.common.BusinessException
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component

@Component
class McpToolRegistry(
    private val posts: PostService,
    private val categories: CategoryService,
    private val comments: CommentService,
    private val profile: BlogProfileService,
    private val mapper: ObjectMapper,
    private val validator: Validator
) {
    private val schemaValidator = DefaultJsonSchemaValidator(mapper)
    private val jsonMapper = JacksonMcpJsonMapper(mapper)

    fun specifications(): List<McpStatelessServerFeatures.SyncToolSpecification> = listOf(
        tool("blog_list_posts", "Search blog posts. page is zero-based; size defaults to 20 (maximum 100).",
            McpToolSchemas.pagination + mapOf(
                "keyword" to McpToolSchemas.text(200), "categoryName" to McpToolSchemas.text(100),
                "tagName" to McpToolSchemas.text(50)
            )) { args, _ ->
            page(posts.searchPosts(
                args["keyword"] as String?, args["categoryName"] as String?, args["tagName"] as String?,
                pageable(args)
            ))
        },
        tool("blog_get_post", "Read full Markdown and editing fields without increasing the view count.",
            mapOf("id" to McpToolSchemas.id), listOf("id")) { args, _ -> posts.getPostForAdmin(id(args)) },
        tool("blog_create_post", "Publish a blog post immediately. Obtain user approval before publishing.",
            mapOf("post" to McpToolSchemas.post), listOf("post"), write = true) { args, identity ->
            mapOf("id" to posts.createPost(body(args, "post", PostSaveRequest::class.java), identity.email))
        },
        tool("blog_update_post",
            "Replace a published post. Read blog_get_post first; preserve categoryId and tags. " +
                "Removed embedded images are deleted. Obtain user approval before updating.",
            mapOf("id" to McpToolSchemas.id, "post" to McpToolSchemas.post), listOf("id", "post"),
            write = true, destructive = true) { args, _ ->
            mapOf("id" to posts.updatePost(id(args), body(args, "post", PostSaveRequest::class.java)))
        },
        tool("blog_delete_post", "Permanently delete a post, its comments and embedded images. Requires confirmation.",
            deletionProperties(), listOf("id", "confirm"), write = true, destructive = true) { args, _ ->
            posts.deletePost(id(args))
            mapOf("deleted" to true)
        },
        tool("blog_list_categories", "Read the category tree.") { _, _ ->
            mapOf("categories" to categories.getCategoryTree())
        },
        tool("blog_create_category", "Create a category. parentId null creates a root category.",
            mapOf("category" to McpToolSchemas.category), listOf("category"), write = true) { args, _ ->
            mapOf("id" to categories.createCategory(body(args, "category", CategoryCreateRequest::class.java)))
        },
        tool("blog_update_category", "Rename or move a category. parentId null moves it to the root.",
            mapOf("id" to McpToolSchemas.id, "category" to McpToolSchemas.category), listOf("id", "category"),
            write = true) { args, _ ->
            categories.updateCategory(id(args), body(args, "category", CategoryUpdateRequest::class.java))
            mapOf("updated" to true)
        },
        tool("blog_delete_category",
            "Delete a category and all descendants. Their posts become uncategorized. Requires confirmation.",
            deletionProperties(), listOf("id", "confirm"), write = true, destructive = true) { args, _ ->
            categories.deleteCategory(id(args))
            mapOf("deleted" to true)
        },
        tool("blog_list_comments", "List recent comments with post titles and slugs. page is zero-based.",
            McpToolSchemas.pagination) { args, _ -> page(comments.getAllComments(pageable(args))) },
        tool("blog_create_comment", "Publish a comment or reply as the administrator who issued this key.",
            mapOf("comment" to McpToolSchemas.comment), listOf("comment"), write = true) { args, identity ->
            mapOf("id" to comments.createComment(body(args, "comment", CommentSaveRequest::class.java), identity.email))
        },
        tool("blog_delete_comment", "Permanently delete a comment and its replies. Requires confirmation.",
            deletionProperties(), listOf("id", "confirm"), write = true, destructive = true) { args, _ ->
            comments.deleteCommentByAdmin(id(args))
            mapOf("deleted" to true)
        },
        tool("blog_get_profile", "Read the public blog profile.") { _, _ -> profile.getProfileForAdmin() },
        tool("blog_update_profile", "Replace the public blog profile. The previous image is deleted when replaced.",
            mapOf("profile" to McpToolSchemas.profile), listOf("profile"), write = true,
            destructive = true) { args, _ ->
            profile.updateProfile(body(args, "profile", ProfileUpdateRequest::class.java))
            mapOf("updated" to true)
        }
    )

    private fun tool(
        name: String,
        description: String,
        properties: Map<String, Any> = emptyMap(),
        required: List<String> = emptyList(),
        write: Boolean = false,
        destructive: Boolean = false,
        handler: (Map<String, Any?>, McpIdentity) -> Any
    ): McpStatelessServerFeatures.SyncToolSpecification {
        val schema = McpToolSchemas.obj(properties, *required.toTypedArray())
        val definition = McpSchema.Tool.builder().name(name).description(description)
            .inputSchema(jsonMapper, mapper.writeValueAsString(schema))
            .annotations(McpSchema.ToolAnnotations(null, !write, destructive, !write, false, false)).build()
        return McpStatelessServerFeatures.SyncToolSpecification(definition) { context, request ->
            val identity = context.get(IDENTITY_CONTEXT) as? McpIdentity
            if (identity == null) {
                error("관리자 인증이 필요합니다.")
            } else if (write && identity.permission != McpPermission.READ_WRITE) {
                error("읽기 전용 연결 키로는 변경할 수 없습니다.")
            } else {
                try {
                    val args = request.arguments() ?: emptyMap()
                    if (!schemaValidator.validate(schema, args).valid()) {
                        error("입력 형식이 올바르지 않습니다. 도구 스키마의 필수 항목, 자료형과 범위를 확인해주세요.")
                    } else {
                        val result = handler(args, identity)
                        if (write) log.info("MCP mutation completed: tool={}, keyId={}", name, identity.keyId)
                        McpSchema.CallToolResult.builder().isError(false)
                            .addTextContent(mapper.writeValueAsString(result)).structuredContent(result).build()
                    }
                } catch (exception: BusinessException) {
                    error(exception.message)
                } catch (_: IllegalArgumentException) {
                    error("입력값을 확인해주세요.")
                } catch (exception: Exception) {
                    // Log no arguments or exception messages: they may contain content or credentials.
                    log.error("MCP tool failed: tool={}, keyId={}, type={}", name, identity.keyId,
                        exception.javaClass.simpleName)
                    error("작업을 완료하지 못했습니다. 관리자에게 문의해주세요.")
                }
            }
        }
    }

    private fun <T : Any> body(args: Map<String, Any?>, key: String, type: Class<T>): T {
        val value = mapper.convertValue(args[key], type)
        val violations = validator.validate(value)
        if (violations.isNotEmpty()) {
            throw BusinessException(violations.map { it.message }.distinct().sorted().joinToString(" "))
        }
        return value
    }

    private fun pageable(args: Map<String, Any?>) = PageRequest.of(
        (args["page"] as? Number)?.toInt() ?: 0, (args["size"] as? Number)?.toInt() ?: 20,
        Sort.by(Sort.Direction.DESC, "createdAt", "id")
    )

    private fun page(page: Page<*>) = mapOf(
        "content" to page.content, "page" to page.number, "size" to page.size,
        "totalElements" to page.totalElements, "totalPages" to page.totalPages
    )

    private fun id(args: Map<String, Any?>) = (args.getValue("id") as Number).toLong()
    private fun deletionProperties() = mapOf("id" to McpToolSchemas.id, "confirm" to McpToolSchemas.confirmation)
    private fun error(message: String) = McpSchema.CallToolResult.builder()
        .isError(true).addTextContent(message).build()

    companion object {
        const val IDENTITY_CONTEXT = "blog.mcp.identity"
        private val log = LoggerFactory.getLogger(McpToolRegistry::class.java)
    }
}
