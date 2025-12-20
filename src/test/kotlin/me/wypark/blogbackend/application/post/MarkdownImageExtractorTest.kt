package me.wypark.blogbackend.application.post

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class MarkdownImageExtractorTest {

    @Test
    fun `extracts file names from markdown image urls in order`() {
        val content = """
            text ![first](https://cdn.example.com/a.png)
            ![second](/images/b.webp) and ![local](c.jpg)
        """.trimIndent()

        assertEquals(
            listOf("a.png", "b.webp", "c.jpg"),
            MarkdownImageExtractor.extractFileNames(content)
        )
    }
}
