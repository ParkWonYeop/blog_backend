package me.wypark.blogbackend.application.image

import java.io.InputStream

interface ImageStorage {
    fun upload(key: String, contentType: String?, size: Long, content: InputStream): String
    fun delete(key: String)
}
