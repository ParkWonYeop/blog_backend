package me.wypark.blogbackend.core.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import java.net.URI

@Configuration
class S3Config(
    @Value("\${spring.cloud.aws.credentials.access-key:admin}") private val accessKey: String,
    @Value("\${spring.cloud.aws.credentials.secret-key:password}") private val secretKey: String,
    @Value("\${spring.cloud.aws.region.static:ap-northeast-2}") private val regionStr: String, // 변수명 regionStr 확인
    @Value("\${spring.cloud.aws.s3.endpoint:http://minio:9000}") private val endpoint: String
) {

    @Bean
    fun s3Client(): S3Client {
        return S3Client.builder()
            .region(Region.of(regionStr))
            .credentialsProvider(
                StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey))
            )
            .endpointOverride(URI.create(endpoint)) // MinIO 주소
            .forcePathStyle(true) // MinIO 필수 설정
            .build()
    }
}