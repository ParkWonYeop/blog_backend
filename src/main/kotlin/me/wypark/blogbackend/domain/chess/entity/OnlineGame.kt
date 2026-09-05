package me.wypark.blogbackend.domain.chess.entity

import java.time.Duration
import java.time.Instant

enum class TimeControl(val initialSeconds: Int, val incrementSeconds: Int, val label: String) {
    BLITZ_1(60, 0, "블리츠 1분"),
    BLITZ_3(180, 0, "블리츠 3분"),
    RAPID_10(600, 0, "래피드 10분"),
    RAPID_15_10(900, 10, "래피드 15|10"),
    RAPID_30_15(1800, 15, "래피드 30|15");

    val initialMillis: Long get() = initialSeconds * 1000L
    val incrementMillis: Long get() = incrementSeconds * 1000L

    companion object {
        fun from(value: String?): TimeControl {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("지원하지 않는 시간 설정입니다.")
        }
    }
}

data class OnlinePlayer(
    val memberId: Long,
    val nickname: String
)

data class OnlineInvite(
    val code: String,
    val host: OnlinePlayer,
    val timeControl: TimeControl,
    val createdAt: Instant
)

data class OnlineGame(
    val gameId: String,
    val timeControl: TimeControl,
    val white: OnlinePlayer,
    val black: OnlinePlayer,
    val moves: List<String>,
    val fen: String,
    val turn: ChessSide,
    val status: String,
    val result: String?,
    val pgn: String,
    val whiteMillis: Long,
    val blackMillis: Long,
    /** 마지막 수를 둔 시각. 백의 첫 수 전에는 null이며 그동안 시계는 멈춰 있다. */
    val lastMoveAt: Instant?,
    val drawOfferedBy: ChessSide?,
    val disconnected: Set<ChessSide>,
    /** 차례인 쪽이 끊긴 채로 이 시각을 넘기면 기권 처리한다. */
    val forfeitDeadlineAt: Instant?,
    val createdAt: Instant,
    val finishedAt: Instant?
) {
    val isActive: Boolean get() = status == IN_PROGRESS

    val clockRunning: Boolean get() = isActive && lastMoveAt != null

    fun sideOf(memberId: Long): ChessSide? = when (memberId) {
        white.memberId -> ChessSide.WHITE
        black.memberId -> ChessSide.BLACK
        else -> null
    }

    fun player(side: ChessSide): OnlinePlayer = if (side == ChessSide.WHITE) white else black

    fun millisOf(side: ChessSide): Long = if (side == ChessSide.WHITE) whiteMillis else blackMillis

    fun remainingMillis(side: ChessSide, now: Instant): Long {
        val stored = millisOf(side)
        val started = lastMoveAt
        if (!isActive || started == null || side != turn) return stored
        return (stored - Duration.between(started, now).toMillis()).coerceAtLeast(0)
    }

    fun withMillis(side: ChessSide, millis: Long): OnlineGame {
        return if (side == ChessSide.WHITE) copy(whiteMillis = millis) else copy(blackMillis = millis)
    }

    companion object {
        const val IN_PROGRESS = "IN_PROGRESS"
        const val RESIGNED = "RESIGNED"
        const val TIMEOUT = "TIMEOUT"
        const val ABANDONED = "ABANDONED"
        const val DRAW_AGREED = "DRAW_AGREED"
        const val ABORTED = "ABORTED"
        const val DRAW_RESULT = "1/2-1/2"
    }
}
