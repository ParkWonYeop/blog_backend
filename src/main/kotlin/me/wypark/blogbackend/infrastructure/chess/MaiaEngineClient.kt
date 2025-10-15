package me.wypark.blogbackend.infrastructure.chess

import me.wypark.blogbackend.application.chess.MaiaEngine
import me.wypark.blogbackend.application.chess.MaiaPlayRequest
import me.wypark.blogbackend.application.chess.MaiaPlayResponse
import me.wypark.blogbackend.application.chess.MaiaStateRequest
import me.wypark.blogbackend.application.chess.MaiaStateResponse
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

