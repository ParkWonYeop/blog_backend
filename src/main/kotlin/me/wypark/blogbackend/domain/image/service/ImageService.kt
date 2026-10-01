package me.wypark.blogbackend.domain.image.service

import me.wypark.blogbackend.global.common.BusinessException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.util.UUID

@Service
class ImageService(
    private val imageStorage: ImageStorage
) {

    fun uploadImage(file: MultipartFile): String {
        if (file.isEmpty) throw BusinessException("업로드할 이미지가 비어 있습니다.")
        if (file.size > MAX_IMAGE_BYTES) {
            throw BusinessException("이미지는 10MB 이하만 업로드할 수 있습니다.")
        }

        val bytes = file.bytes
        val imageType = ImageType.detect(bytes)
            ?: throw BusinessException("JPEG, PNG, GIF, WebP 이미지만 업로드할 수 있습니다.")
        val key = "${UUID.randomUUID()}.${imageType.extension}"

        return ByteArrayInputStream(bytes).use { content ->
            imageStorage.upload(key, imageType.contentType, bytes.size.toLong(), content)
        }
    }

    fun deleteImage(fileName: String) {
        try {
            imageStorage.delete(fileName)
        } catch (exception: Exception) {
            log.error("Failed to delete image from object storage: {}", fileName, exception)
        }
    }

    companion object {
        private const val MAX_IMAGE_BYTES = 10L * 1024 * 1024
        private val log = LoggerFactory.getLogger(ImageService::class.java)
    }

    private enum class ImageType(val extension: String, val contentType: String) {
        JPEG("jpg", "image/jpeg"),
        PNG("png", "image/png"),
        GIF("gif", "image/gif"),
        WEBP("webp", "image/webp");

        companion object {
            fun detect(bytes: ByteArray): ImageType? = when {
                bytes.startsWith(0xFF, 0xD8, 0xFF) -> JPEG
                bytes.startsWith(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) -> PNG
                bytes.startsWithAscii("GIF87a") || bytes.startsWithAscii("GIF89a") -> GIF
                bytes.startsWithAscii("RIFF") && bytes.hasAsciiAt(8, "WEBP") -> WEBP
                else -> null
            }

            private fun ByteArray.startsWith(vararg expected: Int): Boolean {
                return size >= expected.size && expected.indices.all { this[it].toInt() and 0xFF == expected[it] }
            }

            private fun ByteArray.startsWithAscii(expected: String): Boolean = hasAsciiAt(0, expected)

            private fun ByteArray.hasAsciiAt(offset: Int, expected: String): Boolean {
                if (size < offset + expected.length) return false
                return expected.indices.all { this[offset + it].toInt() == expected[it].code }
            }
        }
    }
}
