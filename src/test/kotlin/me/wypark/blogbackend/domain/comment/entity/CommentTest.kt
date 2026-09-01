package me.wypark.blogbackend.domain.comment.entity

import me.wypark.blogbackend.domain.post.entity.Post
import me.wypark.blogbackend.domain.user.entity.Member
import me.wypark.blogbackend.domain.user.entity.Role
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class CommentTest {

    private val author = Member(
        email = "author@example.com",
        password = "encoded",
        nickname = "author",
        role = Role.ROLE_ADMIN
    )
    private val post = Post("title", "content", "slug", member = author)

    @Test
    fun `reply association and author name are derived consistently`() {
        val parent = Comment("parent", post, guestNickname = "guest")
        val reply = Comment("reply", post, member = author)

        parent.addReply(reply)

        assertSame(parent, reply.parent)
        assertEquals(listOf(reply), parent.children)
        assertEquals("guest", parent.getAuthorName())
        assertEquals("author", reply.getAuthorName())
    }
}
