package me.wypark.blogbackend.infrastructure.image

import me.wypark.blogbackend.application.image.ImageStorage
import me.wypark.blogbackend.core.config.AwsProperties
import me.wypark.blogbackend.core.config.ImageProperties
import org.springframework.stereotype.Component
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.S3Exception
import java.io.InputStream

@Component
class S3ImageStorage(
    private val s3Client: S3Client,
    private val awsProperties: AwsProperties,
    imageProperties: ImageProperties
) : ImageStorage {

    private val bucket = awsProperties.s3.bucket

    init {
        if (imageProperties.initializeBucket) createBucketIfMissing()
    }

    override fun upload(key: String, contentType: String?, size: Long, content: InputStream): String {
        val request = PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .contentType(contentType)
            .build()

        s3Client.putObject(request, RequestBody.fromInputStream(content, size))
        return "${awsProperties.s3.endpoint}/$bucket/$key"
    }

