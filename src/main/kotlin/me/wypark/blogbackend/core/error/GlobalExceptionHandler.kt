package me.wypark.blogbackend.core.error

import me.wypark.blogbackend.api.common.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    // 1. 비즈니스 로직 에러 (의도적인 throw)
    @ExceptionHandler(IllegalArgumentException::class, IllegalStateException::class)
    fun handleBusinessException(e: RuntimeException): ResponseEntity<ApiResponse<Nothing>> {
        return ResponseEntity
            .badRequest()
            .body(ApiResponse.error(e.message ?: "잘못된 요청입니다."))
    }

    // 2. @Valid 검증 실패 (DTO 유효성 체크)
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(e: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Nothing>> {
        val message = e.bindingResult.fieldErrors.firstOrNull()?.defaultMessage ?: "입력값이 올바르지 않습니다."
        return ResponseEntity
            .badRequest()
            .body(ApiResponse.error(message))
    }

    // 3. 나머지 알 수 없는 에러
    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ApiResponse<Nothing>> {
        e.printStackTrace() // 로그 남기기
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("서버 내부 오류가 발생했습니다."))
    }
}