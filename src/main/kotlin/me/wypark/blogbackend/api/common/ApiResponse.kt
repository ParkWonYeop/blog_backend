package me.wypark.blogbackend.api.common

/**
 * [API 공통 응답 규격]
 *
 * 클라이언트(Frontend)와 서버 간의 통신 프로토콜을 통일하기 위한 Wrapper 클래스입니다.
 * 모든 REST API 응답은 이 클래스로 감싸서 반환되며, 이를 통해 예외 발생 시에도
 * 일관된 JSON 구조를 보장하여 클라이언트의 에러 핸들링 복잡도를 낮춥니다.
 *
 * @param T 실제 응답 데이터의 타입 (Generic)
 */
data class ApiResponse<T>(
    // 비즈니스 로직 처리 결과 코드 (HTTP Status와는 별개로 세부적인 에러 코드를 정의하여 사용 가능)
    val code: String = "SUCCESS",

    // 클라이언트에게 노출할 알림 메시지 (Toast UI 등에서 활용)
    val message: String = "요청이 성공했습니다.",

    // 실제 전송할 데이터 Payload (실패 시 null)
    val data: T? = null
) {
    companion object {
        /**
         * 성공 응답을 생성하는 정적 팩토리 메서드입니다.
         * 데이터가 없는 경우(예: 삭제/수정 완료)에도 일관된 형식을 유지하기 위해 기본 메시지를 제공합니다.
         */
        fun <T> success(data: T? = null, message: String = "요청이 성공했습니다."): ApiResponse<T> {
            return ApiResponse("SUCCESS", message, data)
        }

        /**
         * 실패 응답을 생성하는 정적 팩토리 메서드입니다.
         * 에러 상황에서는 data 필드가 불필요하므로, <Nothing> 타입을 사용하여
         * 타입 안정성(Type Safety)을 확보하고 불필요한 객체 생성을 방지합니다.
         */
        fun error(message: String, code: String = "ERROR"): ApiResponse<Nothing> {
            return ApiResponse(code, message, null)
        }
    }
}