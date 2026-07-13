package me.wypark.blogbackend.domain.category

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class CategoryTest {

    @Test
    fun `adding and moving a child keeps both sides consistent`() {
        val firstParent = Category("first")
        val secondParent = Category("second")
        val child = Category("child")

        firstParent.addChild(child)
        assertSame(firstParent, child.parent)
        assertEquals(listOf(child), firstParent.children)

        child.changeParent(secondParent)
        assertEquals(emptyList(), firstParent.children)
        assertEquals(listOf(child), secondParent.children)
        assertSame(secondParent, child.parent)

        child.changeParent(null)
        assertNull(child.parent)
        assertEquals(emptyList(), secondParent.children)
    }
}
