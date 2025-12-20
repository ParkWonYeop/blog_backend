package me.wypark.blogbackend.application.image

import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockMultipartFile
import java.io.InputStream
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ImageServiceTest {

    @Test
    fun `upload delegates metadata and content to storage`() {
        val storage = RecordingImageStorage()
        val service = ImageService(storage)
        val file = MockMultipartFile("image", "photo.png", "image/png", byteArrayOf(1, 2, 3))

        val url = service.uploadImage(file)

        assertTrue(storage.key.endsWith(".png"))
        assertEquals("image/png", storage.contentType)
        assertEquals(3L, storage.size)
        assertContentEquals(byteArrayOf(1, 2, 3), storage.content)
        assertEquals("https://images.example/${storage.key}", url)
    }

    @Test
    fun `delete remains fail safe when storage cleanup fails`() {
        val service = ImageService(object : ImageStorage {
            override fun upload(key: String, contentType: String?, size: Long, content: InputStream) = ""
            override fun delete(key: String) = error("storage unavailable")
        })

        service.deleteImage("orphan.png")
    }

    private class RecordingImageStorage : ImageStorage {
        lateinit var key: String
        var contentType: String? = null
        var size: Long = 0
        var content: ByteArray = byteArrayOf()

        override fun upload(key: String, contentType: String?, size: Long, content: InputStream): String {
            this.key = key
            this.contentType = contentType
            this.size = size
            this.content = content.readAllBytes()
            return "https://images.example/$key"
        }

        override fun delete(key: String) = Unit
    }
}
