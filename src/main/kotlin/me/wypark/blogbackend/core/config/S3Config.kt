package me.wypark.blogbackend.core.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import java.net.URI

@Configuration
class S3Config(
    private val properties: AwsProperties
) {

    @Bean
    fun s3Client(): S3Client {
        val credentials = properties.credentials
        return S3Client.builder()
            .region(Region.of(properties.region.static))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(credentials.accessKey, credentials.secretKey)
                )
            )
            .endpointOverride(URI.create(properties.s3.endpoint))
            .forcePathStyle(true)
            .build()
    }
}
