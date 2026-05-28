package me.wypark.blogbackend.domain.chess

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import me.wypark.blogbackend.domain.common.BaseTimeEntity

@Entity
@Table(name = "chess_puzzle")
class ChessPuzzle(
    @Column(nullable = false, unique = true, length = 32)
    val sourcePuzzleId: String,

    @Column(nullable = false)
    val sourceUrl: String,

    @Column(nullable = false, length = 100)
    val title: String,

    @Column(nullable = false, length = 80)
    val theme: String,

    @Column(nullable = false, length = 120)
    val fen: String,

    @Column(nullable = false, length = 20)
    val answer: String,

    @Column(nullable = false, length = 8)
    val answerUci: String,

    @Column(nullable = false)
    val hint: String,

    @Column(nullable = false)
    val rating: Int,

    @Column(nullable = false)
    val popularity: Int,

    @Column(nullable = false)
    val sortOrder: Int,

    @Column(nullable = false)
    val active: Boolean = true
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
}
