package me.wypark.blogbackend.api.controller

import me.wypark.blogbackend.domain.chess.ChessPuzzle
import me.wypark.blogbackend.domain.chess.ChessPuzzleRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ChessPuzzleControllerIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var chessPuzzleRepository: ChessPuzzleRepository

    @Autowired
    lateinit var clock: MutableClock

    @BeforeEach
    fun setUp() {
        clock.setDate(LocalDate.of(2026, 5, 28), ZoneId.of("Asia/Seoul"))
    }

    @Test
    fun `today puzzle is public and returns selected puzzle`() {
        savePuzzle(sourcePuzzleId = "puzzle-1", sortOrder = 1, answer = "Be8#", answerUci = "d7e8")

        mockMvc.perform(get("/api/chess-puzzles/today").param("timezone", "Asia/Seoul"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.data.date").value("2026-05-28"))
            .andExpect(jsonPath("$.data.title").value("오늘의 메이트"))
            .andExpect(jsonPath("$.data.theme").value("mateIn1"))
            .andExpect(jsonPath("$.data.answer").value("Be8#"))
            .andExpect(jsonPath("$.data.answerUci").value("d7e8"))
    }

    @Test
    fun `today puzzle rotates by date`() {
        savePuzzle(sourcePuzzleId = "puzzle-1", sortOrder = 1, answer = "Be8#", answerUci = "d7e8")
        savePuzzle(sourcePuzzleId = "puzzle-2", sortOrder = 2, answer = "Qh5#", answerUci = "g6h5")

