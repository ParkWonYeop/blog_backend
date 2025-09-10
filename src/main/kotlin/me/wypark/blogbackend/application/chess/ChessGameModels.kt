package me.wypark.blogbackend.application.chess

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Pattern
import me.wypark.blogbackend.domain.chess.ChessGameRecord
import me.wypark.blogbackend.domain.chess.ChessGameSession
import java.time.LocalDateTime

data class ChessGameCreateRequest(
    @field:Min(600, message = "레이팅은 600 이상이어야 합니다.")
    @field:Max(2600, message = "레이팅은 2600 이하여야 합니다.")
    val rating: Int = 1500,

    @field:Pattern(
        regexp = "(?i)^(white|black)$",
        message = "playerColor는 white 또는 black이어야 합니다."
    )
    val playerColor: String = "white",

    @field:Pattern(
        regexp = "^(3m|5m|23m|79m)$",
        message = "model은 3m, 5m, 23m, 79m 중 하나여야 합니다."
    )
    val model: String = "23m",

    @field:DecimalMin(value = "0.0", message = "temperature는 0.0 이상이어야 합니다.")
    @field:DecimalMax(value = "2.0", message = "temperature는 2.0 이하여야 합니다.")
    val temperature: Double = 0.8,

    @field:DecimalMin(value = "0.0", message = "topP는 0.0 이상이어야 합니다.")
    @field:DecimalMax(value = "1.0", message = "topP는 1.0 이하여야 합니다.")
    val topP: Double = 0.95
)

