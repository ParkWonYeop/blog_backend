package me.wypark.blogbackend.domain.mcp.controller

internal object McpToolSchemas {
    fun text(max: Int, nullable: Boolean = false) = mapOf(
        "type" to if (nullable) listOf("string", "null") else "string", "maxLength" to max
    )

    fun integer(min: Long, max: Long = 9_007_199_254_740_991, nullable: Boolean = false) = mapOf(
        "type" to if (nullable) listOf("integer", "null") else "integer",
        "minimum" to min, "maximum" to max
    )

    val id = integer(1)
    val nullableId = integer(1, nullable = true)
    val confirmation = mapOf(
        "type" to "boolean", "const" to true,
        "description" to "Set true only after the user has explicitly approved this deletion."
    )
    val pagination = mapOf("page" to integer(0, 100_000), "size" to integer(1, 100))

    fun obj(properties: Map<String, Any>, vararg required: String): Map<String, Any> = mapOf(
        "type" to "object", "properties" to properties,
        "required" to required.toList(), "additionalProperties" to false
    )

    val post = obj(mapOf(
        "title" to text(200), "content" to text(1_000_000), "slug" to text(200, true),
        "categoryId" to nullableId,
        "tags" to mapOf("type" to "array", "maxItems" to 50,
            "items" to (text(50) + mapOf("minLength" to 1, "pattern" to "\\S")))
    ), "title", "content", "categoryId", "tags")

    val category = obj(mapOf("name" to text(100), "parentId" to nullableId), "name", "parentId")
    val comment = obj(mapOf(
        "postSlug" to text(200), "content" to text(2_000), "parentId" to nullableId
    ), "postSlug", "content")
    val profile = obj(mapOf(
        "name" to text(100), "bio" to text(2_000), "imageUrl" to text(2_048, true),
        "githubUrl" to text(2_048, true), "email" to text(254, true)
    ), "name", "bio", "imageUrl", "githubUrl", "email")
}
