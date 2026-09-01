package me.wypark.blogbackend.domain.chess.repository
import me.wypark.blogbackend.domain.chess.entity.ChessPuzzle

import org.springframework.data.jpa.repository.JpaRepository

interface ChessPuzzleRepository : JpaRepository<ChessPuzzle, Long> {

    fun findAllByActiveTrueOrderBySortOrderAscIdAsc(): List<ChessPuzzle>
}
