package me.wypark.blogbackend.application.common

class BusinessException(
    override val message: String,
    val code: String = DEFAULT_CODE
) : RuntimeException(message) {

    companion object {
        const val DEFAULT_CODE = "ERROR"
    }
}
