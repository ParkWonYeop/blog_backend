package me.wypark.blogbackend.global.config

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

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
@Validated
data class AwsProperties(
    @field:Valid
    val credentials: Credentials = Credentials(),
    @field:Valid
    val region: AwsRegion = AwsRegion(),
    @field:Valid
    val s3: S3 = S3()
) {
    data class Credentials(
        @field:NotBlank
        val accessKey: String = "",
        @field:NotBlank
        val secretKey: String = ""
    )

    data class AwsRegion(
        val static: String = "ap-northeast-2"
    )

    data class S3(
        @field:NotBlank
        val endpoint: String = "http://minio:9000",
        @field:NotBlank
        val bucket: String = "blog-images"
    )
}

@ConfigurationProperties("blog.image")
data class ImageProperties(
    val initializeBucket: Boolean = true
)
