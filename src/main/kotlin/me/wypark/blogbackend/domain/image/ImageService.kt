package me.wypark.blogbackend.domain.image

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.util.*

/**
 * [이미지 처리 서비스]
 *
 * AWS S3 또는 호환 가능한 Object Storage(MinIO 등)와의 통신을 전담하는 서비스입니다.
 * 비즈니스 로직(게시글 작성 등)에서 파일 저장에 대한 세부 구현을 몰라도 되도록
 * 업로드 및 삭제 기능을 추상화하여 제공합니다.
 */
@Service
class ImageService(
    private val s3Client: S3Client,
    @Value("\${spring.cloud.aws.s3.endpoint:http://minio:9000}") private val endpoint: String,
    @Value("\${blog.image.initialize-bucket:true}") private val initializeBucket: Boolean
) {
    private val bucketName = "blog-images"

    /**
     * 서비스 초기화 시점에 버킷 존재 여부를 확인합니다.
     * 로컬 개발 환경이나 초기 배포 시, 수동으로 스토리지를 세팅하는 번거로움을 줄이기 위해
     * 애플리케이션 레벨에서 인프라(Bucket & Policy)를 자동 프로비저닝(Auto-Provisioning)합니다.
     */
    init {
        if (initializeBucket) {
            createBucketIfNotExists()
        }
    }

    /**
     * 이미지를 스토리지에 업로드하고 접근 가능한 URL을 반환합니다.
     *
     * [파일명 생성 전략]
     * 사용자가 업로드한 원본 파일명은 중복될 가능성이 높으므로,
     * UUID(Universally Unique Identifier)를 사용하여 고유한 식별자를 생성함으로써 덮어쓰기(Overwrite)를 방지합니다.
     */
    fun uploadImage(file: MultipartFile): String {
        val originalName = file.originalFilename ?: "image.jpg"
        val ext = originalName.substringAfterLast(".", "jpg")
        val fileName = "${UUID.randomUUID()}.$ext"

        // 메타데이터(ContentType)를 명시하여 브라우저에서 올바르게 렌더링되도록 설정
        val putObjectRequest = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(fileName)
            .contentType(file.contentType)
            .build()

        s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.inputStream, file.size))

        // 클라이언트가 즉시 접근할 수 있는 절대 경로(URL) 반환
        return "$endpoint/$bucketName/$fileName"
    }

    /**
     * 스토리지에서 이미지를 삭제합니다.
     *
     * [Fail-Safe 전략]
     * 이미지 삭제 실패가 비즈니스 트랜잭션(예: 게시글 삭제)의 실패로 이어지지 않도록 예외를 내부에서 소비(Swallow)합니다.
     * 고아 객체(Orphaned Object)가 남더라도 메인 데이터의 정합성을 우선시하는 설계입니다.
     */
    fun deleteImage(fileName: String) {
        try {
            val deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .build()

            s3Client.deleteObject(deleteObjectRequest)
        } catch (e: Exception) {
            e.printStackTrace() // 실제 운영 시에는 Error Log 레벨로 기록하여 추후 배치 작업 등으로 정리 필요
        }
    }

    /**
     * [버킷 초기화 로직]
     * 버킷이 없을 경우 생성하고, 웹에서 이미지를 조회할 수 있도록 'Public Read' 권한 정책을 주입합니다.
     */
    private fun createBucketIfNotExists() {
        try {
            // 버킷 존재 여부 확인 (Head Bucket)
            s3Client.headBucket { it.bucket(bucketName) }
        } catch (e: Exception) {
            // 버킷 생성
            s3Client.createBucket { it.bucket(bucketName) }

            // [접근 제어 정책 설정]
            // 외부 사용자가 URL을 통해 이미지(Object)를 조회(GetObject)할 수 있도록
            // 버킷 정책(Bucket Policy)을 JSON 형태로 정의하여 적용합니다.
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
