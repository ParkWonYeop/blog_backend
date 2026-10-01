package me.wypark.blogbackend.global.error

import jakarta.validation.ConstraintViolationException
import me.wypark.blogbackend.global.common.ApiResponse
import me.wypark.blogbackend.global.common.BusinessException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.web.method.annotation.HandlerMethodValidationException

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(exception: BusinessException): ResponseEntity<ApiResponse<Nothing>> {
        return ResponseEntity
            .status(exception.status)
            .body(ApiResponse.error(exception.message, exception.code))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(e: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Nothing>> {
        val message = e.bindingResult.fieldErrors.firstOrNull()?.defaultMessage
            ?: "입력값이 올바르지 않습니다."
        return ResponseEntity
            .badRequest()
            .body(ApiResponse.error(message))
    }

    @ExceptionHandler(ConstraintViolationException::class, HandlerMethodValidationException::class)
    fun handleConstraintViolation(e: Exception): ResponseEntity<ApiResponse<Nothing>> {
        log.debug("Request validation failed: {}", e.message)
        return ResponseEntity
            .badRequest()
            .body(ApiResponse.error("입력값이 올바르지 않습니다.", "BAD_REQUEST"))
    }

    @ExceptionHandler(HttpMessageNotReadableException::class, MissingServletRequestParameterException::class)
    fun handleMalformedRequest(e: Exception): ResponseEntity<ApiResponse<Nothing>> {
        log.debug("Malformed request: {}", e.message)
        return ResponseEntity
            .badRequest()
            .body(ApiResponse.error("요청 형식이 올바르지 않습니다.", "BAD_REQUEST"))
    }

    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleUploadTooLarge(): ResponseEntity<ApiResponse<Nothing>> {
        return ResponseEntity
            .status(HttpStatus.PAYLOAD_TOO_LARGE)
            .body(ApiResponse.error("업로드 파일은 10MB 이하만 허용됩니다.", "PAYLOAD_TOO_LARGE"))
    }

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ApiResponse<Nothing>> {
        log.error("Error occurred: ", e)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("서버 내부 오류가 발생했습니다."))
    }
}
