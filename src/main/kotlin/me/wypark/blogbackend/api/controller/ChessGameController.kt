package me.wypark.blogbackend.api.controller

import jakarta.validation.Valid
import me.wypark.blogbackend.api.common.ApiResponse
import me.wypark.blogbackend.api.dto.ChessGameCreateRequest
import me.wypark.blogbackend.api.dto.ChessGamePgnResponse
import me.wypark.blogbackend.api.dto.ChessGameResponse
import me.wypark.blogbackend.api.dto.ChessGameStatsResponse
import me.wypark.blogbackend.api.dto.ChessGameSummaryResponse
import me.wypark.blogbackend.api.dto.ChessMoveRequest
import me.wypark.blogbackend.domain.auth.CustomUserDetails
import me.wypark.blogbackend.domain.chess.ChessGameService
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
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @Valid @RequestBody request: ChessGameCreateRequest
    ): ResponseEntity<ApiResponse<ChessGameResponse>> {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(chessGameService.createGame(userDetails.memberId, request)))
    }

    @GetMapping
    fun getGames(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @PageableDefault(size = 20, sort = ["updatedAt"], direction = Sort.Direction.DESC) pageable: Pageable
    ): ResponseEntity<ApiResponse<Page<ChessGameSummaryResponse>>> {
        return ResponseEntity.ok(ApiResponse.success(chessGameService.getGames(userDetails.memberId, pageable)))
    }

    @GetMapping("/stats")
    fun getStats(
        @AuthenticationPrincipal userDetails: CustomUserDetails
    ): ResponseEntity<ApiResponse<ChessGameStatsResponse>> {
        return ResponseEntity.ok(ApiResponse.success(chessGameService.getStats(userDetails.memberId)))
    }

    @GetMapping("/{gameId}")
    fun getGame(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @PathVariable gameId: String
    ): ResponseEntity<ApiResponse<ChessGameResponse>> {
        return ResponseEntity.ok(ApiResponse.success(chessGameService.getGame(userDetails.memberId, gameId)))
    }

    @GetMapping("/{gameId}/pgn")
    fun getPgn(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @PathVariable gameId: String
    ): ResponseEntity<ApiResponse<ChessGamePgnResponse>> {
        return ResponseEntity.ok(ApiResponse.success(chessGameService.getPgn(userDetails.memberId, gameId)))
    }

    @PostMapping("/{gameId}/moves")
    fun playMove(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @PathVariable gameId: String,
        @Valid @RequestBody request: ChessMoveRequest
    ): ResponseEntity<ApiResponse<ChessGameResponse>> {
        return ResponseEntity.ok(ApiResponse.success(chessGameService.playMove(userDetails.memberId, gameId, request)))
    }

    @PostMapping("/{gameId}/resign")
    fun resign(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @PathVariable gameId: String
    ): ResponseEntity<ApiResponse<ChessGameResponse>> {
        return ResponseEntity.ok(ApiResponse.success(chessGameService.resign(userDetails.memberId, gameId)))
    }

    @PostMapping("/{gameId}/undo")
    fun undoMove(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @PathVariable gameId: String
    ): ResponseEntity<ApiResponse<ChessGameResponse>> {
        return ResponseEntity.ok(ApiResponse.success(chessGameService.undoMove(userDetails.memberId, gameId)))
    }
}
