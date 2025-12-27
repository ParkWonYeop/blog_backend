package me.wypark.blogbackend.domain.image

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.util.*

@Service
class ImageService(
    private val s3Client: S3Client,
    @Value("\${spring.cloud.aws.s3.endpoint:http://minio:9000}") private val endpoint: String
) {
    private val bucketName = "blog-images" // 버킷 이름

    init {
        createBucketIfNotExists()
    }

    fun uploadImage(file: MultipartFile): String {
        // 1. 파일명 중복 방지 (UUID 사용)
        val originalName = file.originalFilename ?: "image.jpg"
        val ext = originalName.substringAfterLast(".", "jpg")
        val fileName = "${UUID.randomUUID()}.$ext"

        // 2. S3(MinIO)로 업로드
        val putObjectRequest = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(fileName)
            .contentType(file.contentType)
            .build()

        s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.inputStream, file.size))

        // 3. 접속 가능한 URL 반환
        return "$endpoint/$bucketName/$fileName"
    }

    // 👈 [추가] 이미지 삭제 로직
    fun deleteImage(fileName: String) {
        try {
            val deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .build()

            s3Client.deleteObject(deleteObjectRequest)
        } catch (e: Exception) {
            // 이미지가 이미 없거나 삭제 실패 시 로그만 남기고 진행 (게시글 삭제 자체를 막지 않기 위해)
            e.printStackTrace()
        }
    }

    private fun createBucketIfNotExists() {
        try {
            // 버킷 존재 여부 확인 (없으면 에러 발생하므로 try-catch)
            s3Client.headBucket { it.bucket(bucketName) }
        } catch (e: Exception) {
            // 버킷 생성
            s3Client.createBucket { it.bucket(bucketName) }

            // ⭐ 버킷을 Public(공개)으로 설정 (이미지 조회를 위해 필수)
            val policy = """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Principal": { "AWS": ["*"] },
                      "Action": ["s3:GetObject"],
                      "Resource": ["arn:aws:s3:::$bucketName/*"]
                    }
                  ]
                }
            """.trimIndent()

            s3Client.putBucketPolicy {
                it.bucket(bucketName).policy(policy)
            }
        }
    }
}