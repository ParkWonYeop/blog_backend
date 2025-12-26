package me.wypark.blogbackend.domain.image

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetUrlRequest
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
        // 로컬 개발 환경에서는 localhost 주소를 직접 조합해서 줍니다.
        // 배포 시에는 실제 도메인이나 CloudFront 주소로 변경해야 합니다.
        return "$endpoint/$bucketName/$fileName"
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