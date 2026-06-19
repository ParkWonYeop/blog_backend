package me.wypark.blogbackend.domain.chess

interface MaiaEngine {

    fun getState(request: MaiaStateRequest): MaiaStateResponse

    fun playMove(request: MaiaPlayRequest): MaiaPlayResponse
}

data class MaiaStateRequest(
    val moves: List<String>,
    val white: String? = null,
    val black: String? = null,
    val event: String = "Maia3"
)

data class MaiaPlayRequest(
    val moves: List<String>,
    val rating: Int,
    val model: String,
    val temperature: Double,
    val topP: Double,
    val white: String? = null,
    val black: String? = null,
    val event: String = "Maia3"
)

data class MaiaStateResponse(
    val fen: String,
    val turn: String,
    val status: String,
    val result: String?,
    val pgn: String = ""
)

data class MaiaPlayResponse(
    val move: String?,
    val fen: String,
    val turn: String,
    val status: String,
    val result: String?,
    val pgn: String = ""
)
