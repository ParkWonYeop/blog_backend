package me.wypark.blogbackend.domain.chess

import org.springframework.data.jpa.repository.JpaRepository

interface ChessPuzzleRepository : JpaRepository<ChessPuzzle, Long> {

    fun findAllByActiveTrueOrderBySortOrderAscIdAsc(): List<ChessPuzzle>
}
