package me.wypark.blogbackend.application.chess

import me.wypark.blogbackend.domain.chess.ChessPuzzleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Service
@Transactional(readOnly = true)
class ChessPuzzleService(
    private val chessPuzzleRepository: ChessPuzzleRepository,
    private val clock: Clock
) {

    fun getTodayPuzzle(timezone: String): ChessPuzzleResponse? {
        val puzzles = chessPuzzleRepository.findAllByActiveTrueOrderBySortOrderAscIdAsc()
        if (puzzles.isEmpty()) return null

        val zoneId = ZoneId.of(timezone)
        val today = LocalDate.now(clock.withZone(zoneId))
        val index = ChessPuzzleSelector.indexFor(today, puzzles.size)
        return ChessPuzzleResponse.from(puzzles[index], today)
    }
}

object ChessPuzzleSelector {
    private val baseDate: LocalDate = LocalDate.of(2026, 5, 28)

    fun indexFor(date: LocalDate, puzzleCount: Int): Int {
        require(puzzleCount > 0) { "퍼즐 개수는 1개 이상이어야 합니다." }

        return Math.floorMod(ChronoUnit.DAYS.between(baseDate, date).toInt(), puzzleCount)
    }
}
