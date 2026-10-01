package me.wypark.blogbackend.domain.image.service

import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockMultipartFile
import java.io.InputStream
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import me.wypark.blogbackend.global.common.BusinessException

class ImageServiceTest {

    @Test
    fun `upload delegates metadata and content to storage`() {
        val storage = RecordingImageStorage()
        val service = ImageService(storage)
        val png = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            1, 2, 3
        )
        val file = MockMultipartFile("image", "payload.svg", "image/svg+xml", png)

        val url = service.uploadImage(file)

        assertTrue(storage.key.endsWith(".png"))
        assertEquals("image/png", storage.contentType)
        assertEquals(png.size.toLong(), storage.size)
        assertContentEquals(png, storage.content)
        assertEquals("https://images.example/${storage.key}", url)
    }

    @Test
    fun `upload rejects content whose bytes are not a supported image`() {
        val service = ImageService(RecordingImageStorage())
        val file = MockMultipartFile("image", "photo.png", "image/png", "not an image".toByteArray())

        assertFailsWith<BusinessException> { service.uploadImage(file) }
    }

    @Test
    fun `delete remains fail safe when storage cleanup fails`() {
        val service = ImageService(object : ImageStorage {
            override fun upload(key: String, contentType: String, size: Long, content: InputStream) = ""
            override fun delete(key: String) = error("storage unavailable")
        })

        service.deleteImage("orphan.png")
    }

    private class RecordingImageStorage : ImageStorage {
        lateinit var key: String
        var contentType: String? = null
        var size: Long = 0
        var content: ByteArray = byteArrayOf()

        override fun upload(key: String, contentType: String, size: Long, content: InputStream): String {
            this.key = key
            this.contentType = contentType
            this.size = size
            this.content = content.readAllBytes()
            return "https://images.example/$key"
        }

        override fun delete(key: String) = Unit
    }
}
