package me.wypark.blogbackend.application.post

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SlugGeneratorTest {

    @Test
    fun `uses normalized title when explicit slug is absent`() {
        val slug = SlugGenerator.generate(null, "  Hello   Kotlin  ") { false }

        assertEquals("hello-kotlin", slug)
    }

    @Test
    fun `preserves legacy raw suffix format after a sanitized candidate collision`() {
        val taken = setOf("helloworld")

        val slug = SlugGenerator.generate("hello?;world", "ignored") { it in taken }

        assertEquals("hello?;world-1", slug)
    }
}
