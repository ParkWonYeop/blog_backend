package me.wypark.blogbackend.core.error

import me.wypark.blogbackend.api.common.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * [전역 예외 처리 핸들러]
 *
 * 애플리케이션 전반에서 발생하는 예외(Exception)를 중앙에서 캡처하여
 * 클라이언트에게 일관된 포맷(ApiResponse)의 에러 응답을 반환합니다.
 * @RestControllerAdvice를 사용하여 모든 컨트롤러에 AOP(Aspect Oriented Programming) 방식으로 적용됩니다.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    /**
     * [비즈니스 로직 예외 처리]
     *
     * 서비스 계층(Service Layer)에서 검증 로직 수행 중 의도적으로 발생시킨 예외를 처리합니다.
     * 예: 중복된 이메일, 존재하지 않는 게시글 조회 등
     * 이는 클라이언트의 잘못된 요청(Bad Request)으로 간주하여 400 상태 코드를 반환합니다.
     */
    @ExceptionHandler(IllegalArgumentException::class, IllegalStateException::class)
    fun handleBusinessException(e: RuntimeException): ResponseEntity<ApiResponse<Nothing>> {
        return ResponseEntity
            .badRequest()
            .body(ApiResponse.error(e.message ?: "잘못된 요청입니다."))
    }

    /**
     * [입력값 유효성 검증 실패 처리]
     *
     * @Valid 어노테이션에 의해 DTO 검증 실패 시 발생하는 예외(MethodArgumentNotValidException)를 처리합니다.
     * 여러 필드에서 에러가 발생할 수 있으나, 클라이언트가 즉시 인지하고 수정할 수 있도록
     * 첫 번째 에러 메시지만 추출하여 간결하게 반환합니다.
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(e: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Nothing>> {
        val message = e.bindingResult.fieldErrors.firstOrNull()?.defaultMessage ?: "입력값이 올바르지 않습니다."
        return ResponseEntity
            .badRequest()
            .body(ApiResponse.error(message))
    }

    /**
     * [시스템 예외 처리 (Fallback)]
     *
     * 명시적으로 처리되지 않은 모든 예외를 잡아내는 최후의 방어선입니다.
     * NullPointerException이나 DB 연결 실패 등 예측하지 못한 서버 내부 오류가 이에 해당합니다.
     *
     * [보안 전략]
     * 내부 로직이 노출될 수 있는 스택 트레이스(Stack Trace)는 클라이언트에게 절대 반환하지 않고 서버 로그로만 남기며,
     * 사용자에게는 일반적인 500 에러 메시지만 전달합니다.
     */
    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ApiResponse<Nothing>> {
        e.printStackTrace() // 실제 운영 환경에서는 SLF4J 등의 로거를 사용하여 파일/ELK로 수집해야 함
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("서버 내부 오류가 발생했습니다."))
    }
}