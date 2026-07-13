package me.wypark.blogbackend.core.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("jwt")
data class JwtProperties(
    val secret: String,
    val accessTokenValidity: Long,
    val refreshTokenValidity: Long
)

@ConfigurationProperties("blog.cors")
data class CorsProperties(
    val allowedOrigins: List<String> = listOf("https://blog.wypark.me")
)

@ConfigurationProperties("spring.cloud.aws")
data class AwsProperties(
    val credentials: Credentials = Credentials(),
    val region: AwsRegion = AwsRegion(),
    val s3: S3 = S3()
) {
    data class Credentials(
        val accessKey: String = "admin",
        val secretKey: String = "password"
    )

    data class AwsRegion(
        val static: String = "ap-northeast-2"
    )

    data class S3(
        val endpoint: String = "http://minio:9000",
        val bucket: String = "blog-images"
    )
}

@ConfigurationProperties("blog.image")
data class ImageProperties(
    val initializeBucket: Boolean = true
)
