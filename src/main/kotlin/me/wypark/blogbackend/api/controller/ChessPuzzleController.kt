package me.wypark.blogbackend.api.controller

import me.wypark.blogbackend.api.common.ApiResponse
import me.wypark.blogbackend.application.chess.ChessPuzzleResponse
import me.wypark.blogbackend.application.chess.ChessPuzzleService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/chess-puzzles")
class ChessPuzzleController(
    private val chessPuzzleService: ChessPuzzleService
) {

    @GetMapping("/today")
    fun getTodayPuzzle(
        @RequestParam(defaultValue = "Asia/Seoul") timezone: String
    ): ResponseEntity<ApiResponse<ChessPuzzleResponse>> {
        val puzzle = chessPuzzleService.getTodayPuzzle(timezone)
            ?: return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(
                    ApiResponse(
                        code = "CHESS_PUZZLE_NOT_FOUND",
                        message = "오늘의 체스 퍼즐이 준비되지 않았습니다.",
                        data = null
                    )
                )

        return ResponseEntity.ok(ApiResponse.success(puzzle))
    }
}
