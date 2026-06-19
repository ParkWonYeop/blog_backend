package me.wypark.blogbackend.domain.chess

import org.slf4j.LoggerFactory
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
            throw IllegalArgumentException("Maia 엔진이 요청을 처리하지 못했습니다.")
        } catch (e: RestClientException) {
            log.error("Failed to call Maia engine", e)
            throw IllegalStateException("Maia 엔진에 연결할 수 없습니다.")
        }
    }
}
