package me.wypark.blogbackend.domain.chess.controller

import me.wypark.blogbackend.domain.chess.dto.OnlineGameResponse
import me.wypark.blogbackend.domain.chess.dto.OnlineGameSummaryResponse
import me.wypark.blogbackend.domain.chess.dto.TimeControlResponse
import me.wypark.blogbackend.domain.chess.entity.TimeControl
import me.wypark.blogbackend.domain.chess.service.OnlineGameService
import me.wypark.blogbackend.global.common.ApiResponse
import me.wypark.blogbackend.global.security.AuthenticatedUser
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** 온라인 대국의 조회 API. 착수·기권 같은 실시간 동작은 /ws/chess WebSocket으로 처리한다. */
@RestController
@RequestMapping("/api/chess/online")
class OnlineChessController(
    private val onlineGameService: OnlineGameService
) {

    @GetMapping("/time-controls")
    fun getTimeControls(): ResponseEntity<ApiResponse<List<TimeControlResponse>>> {
        return ResponseEntity.ok(ApiResponse.success(TimeControl.entries.map(TimeControlResponse::from)))
    }

    @GetMapping("/games")
    fun getGames(
        @AuthenticationPrincipal userDetails: AuthenticatedUser,
        @PageableDefault(size = 20, sort = ["startedAt"], direction = Sort.Direction.DESC) pageable: Pageable
    ): ResponseEntity<ApiResponse<Page<OnlineGameSummaryResponse>>> {
        return ResponseEntity.ok(ApiResponse.success(onlineGameService.listGames(userDetails.memberId, pageable)))
    }

    /** 진행 중인 대국이 없으면 data가 null이다. */
    @GetMapping("/games/active")
    fun getActiveGame(
        @AuthenticationPrincipal userDetails: AuthenticatedUser
    ): ResponseEntity<ApiResponse<OnlineGameResponse?>> {
        return ResponseEntity.ok(ApiResponse.success(onlineGameService.findActiveGame(userDetails.memberId)))
    }

    @GetMapping("/games/{gameId}")
    fun getGame(
        @AuthenticationPrincipal userDetails: AuthenticatedUser,
        @PathVariable gameId: String
    ): ResponseEntity<ApiResponse<OnlineGameResponse>> {
        return ResponseEntity.ok(ApiResponse.success(onlineGameService.getGame(userDetails.memberId, gameId)))
    }
}
