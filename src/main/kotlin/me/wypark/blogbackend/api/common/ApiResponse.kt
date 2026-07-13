package me.wypark.blogbackend.api.common

data class ApiResponse<T>(
    val code: String = "SUCCESS",
    val message: String = "요청이 성공했습니다.",
    val data: T? = null
) {
    companion object {
        fun <T> success(data: T? = null, message: String = "요청이 성공했습니다.") =
            ApiResponse("SUCCESS", message, data)

        fun error(message: String, code: String = "ERROR") =
            ApiResponse<Nothing>(code, message, null)
    }
}
