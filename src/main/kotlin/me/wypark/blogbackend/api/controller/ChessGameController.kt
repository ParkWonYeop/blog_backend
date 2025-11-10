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

    @PostMapping
    fun createGame(
        @AuthenticationPrincipal userDetails: AuthenticatedUser,
        @Valid @RequestBody request: ChessGameCreateRequest
    ): ResponseEntity<ApiResponse<ChessGameResponse>> {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(chessGameService.createGame(userDetails.memberId, request)))
    }

    @GetMapping
    fun getGames(
        @AuthenticationPrincipal userDetails: AuthenticatedUser,
        @PageableDefault(size = 20, sort = ["updatedAt"], direction = Sort.Direction.DESC) pageable: Pageable
    ): ResponseEntity<ApiResponse<Page<ChessGameSummaryResponse>>> {
        return ResponseEntity.ok(ApiResponse.success(chessGameService.getGames(userDetails.memberId, pageable)))
    }

    @GetMapping("/stats")
    fun getStats(
        @AuthenticationPrincipal userDetails: AuthenticatedUser
    ): ResponseEntity<ApiResponse<ChessGameStatsResponse>> {
        return ResponseEntity.ok(ApiResponse.success(chessGameService.getStats(userDetails.memberId)))
    }

    @GetMapping("/{gameId}")
    fun getGame(
        @AuthenticationPrincipal userDetails: AuthenticatedUser,
        @PathVariable gameId: String
    ): ResponseEntity<ApiResponse<ChessGameResponse>> {
        return ResponseEntity.ok(ApiResponse.success(chessGameService.getGame(userDetails.memberId, gameId)))
    }

