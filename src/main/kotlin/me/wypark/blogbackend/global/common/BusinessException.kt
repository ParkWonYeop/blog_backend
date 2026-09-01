package me.wypark.blogbackend.global.common

import org.springframework.http.HttpStatus

class BusinessException(
    override val message: String,
    val code: String = DEFAULT_CODE,
    val status: HttpStatus = HttpStatus.BAD_REQUEST
) : RuntimeException(message) {

    companion object {
        const val DEFAULT_CODE = "ERROR"

        fun notFound(message: String): BusinessException {
            return BusinessException(message, "NOT_FOUND", HttpStatus.NOT_FOUND)
        }
    }
}
