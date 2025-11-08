package me.wypark.blogbackend.api.controller

import jakarta.validation.Valid
import me.wypark.blogbackend.api.common.ApiResponse
import me.wypark.blogbackend.application.auth.AuthenticatedUser
import me.wypark.blogbackend.application.chess.ChessGameCreateRequest
import me.wypark.blogbackend.application.chess.ChessGamePgnResponse
import me.wypark.blogbackend.application.chess.ChessGameResponse
import me.wypark.blogbackend.application.chess.ChessGameService
import me.wypark.blogbackend.application.chess.ChessGameStatsResponse
import me.wypark.blogbackend.application.chess.ChessGameSummaryResponse
import me.wypark.blogbackend.application.chess.ChessMoveRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/chess/games")
class ChessGameController(
    private val chessGameService: ChessGameService
) {

