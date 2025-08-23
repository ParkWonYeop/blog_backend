package me.wypark.blogbackend.application.image

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@Service
class ImageService(
    private val imageStorage: ImageStorage
) {

    fun uploadImage(file: MultipartFile): String {
        val extension = file.originalFilename
            ?.substringAfterLast('.', DEFAULT_EXTENSION)
            ?: DEFAULT_EXTENSION
        val key = "${UUID.randomUUID()}.$extension"

        return file.inputStream.use { content ->
            imageStorage.upload(key, file.contentType, file.size, content)
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
        private const val DEFAULT_EXTENSION = "jpg"
        private val log = LoggerFactory.getLogger(ImageService::class.java)
    }
}
