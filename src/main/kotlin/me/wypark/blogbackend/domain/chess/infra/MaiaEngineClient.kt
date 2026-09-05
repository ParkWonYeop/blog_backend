package me.wypark.blogbackend.domain.chess.infra

import me.wypark.blogbackend.domain.chess.service.MaiaEngine
import me.wypark.blogbackend.domain.chess.service.MaiaPlayRequest
import me.wypark.blogbackend.domain.chess.service.MaiaPlayResponse
import me.wypark.blogbackend.domain.chess.service.MaiaStateRequest
import me.wypark.blogbackend.domain.chess.service.MaiaStateResponse
import me.wypark.blogbackend.global.common.BusinessException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException

@Component
class MaiaEngineClient(
    private val maiaRestClient: RestClient
) : MaiaEngine {

    private val log = LoggerFactory.getLogger(this.javaClass)

    override fun getState(request: MaiaStateRequest): MaiaStateResponse {
        return exchange("/maia/state", request, MaiaStateResponse::class.java)
    }

    override fun playMove(request: MaiaPlayRequest): MaiaPlayResponse {
        return exchange("/maia/move", request, MaiaPlayResponse::class.java)
    }

    private fun <T : Any> exchange(path: String, request: Any, responseType: Class<T>): T {
        try {
            return maiaRestClient
                .post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(responseType)
                ?: throw IllegalStateException("Maia 엔진 응답이 비어 있습니다.")
        } catch (e: RestClientResponseException) {
            log.warn("Maia engine returned {}: {}", e.statusCode, e.responseBodyAsString)
            if (e.statusCode.is5xxServerError) throw engineUnavailable()
            throw BusinessException("Maia 엔진이 요청을 처리하지 못했습니다.", "MAIA_REJECTED")
        } catch (e: RestClientException) {
            log.error("Failed to call Maia engine", e)
            throw engineUnavailable()
        }
    }

    // 엔진이 바쁘거나(503) 죽어 있으면 500 대신 503으로 알려 클라이언트가 재시도하게 한다.
    private fun engineUnavailable(): BusinessException {
        return BusinessException(
            "Maia 엔진이 바쁘거나 응답하지 않습니다. 잠시 후 다시 시도해주세요.",
            "MAIA_UNAVAILABLE",
            HttpStatus.SERVICE_UNAVAILABLE
        )
    }
}
