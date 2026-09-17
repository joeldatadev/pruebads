package com.hoshiraflow.domain.usecase

import com.hoshiraflow.domain.model.PuzzleColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GenerateProceduralLevelUseCaseTest {

    private val generate = GenerateProceduralLevelUseCase()

    private val indicesCubriendoLaCurva = listOf(1, 10, 11, 25, 26, 40, 41, 55, 56, 70, 71, 85, 86, 95, 96, 500)

    @Test
    fun `cada nivel generado tiene exactamente 2 nodos por color y sin celdas repetidas`() {
        for (index in indicesCubriendoLaCurva) {
            for (seed in 1L..5L) {
                val board = generate(seed = seed, index = index)

                val nodesByColor = board.nodes.groupBy { it.color }
                nodesByColor.values.forEach { nodesOfColor ->
                    assertEquals(
                        "index=$index seed=$seed: cada color debe tener exactamente 2 nodos",
                        2,
                        nodesOfColor.size
                    )
                }

                val allCells = board.nodes.map { it.cell }
                assertEquals(
                    "index=$index seed=$seed: no debe haber dos nodos (de cualquier color) en la misma celda",
                    allCells.size,
                    allCells.toSet().size
                )
            }
        }
    }

    @Test
    fun `todas las celdas de los nodos caen dentro del tablero`() {
        for (index in indicesCubriendoLaCurva) {
            val board = generate(seed = 42L, index = index)
            board.nodes.forEach { node ->
                assertTrue(
                    "index=$index: fila ${node.cell.row} fuera de rango (rows=${board.rows})",
                    node.cell.row in 0 until board.rows
                )
                assertTrue(
                    "index=$index: columna ${node.cell.col} fuera de rango (cols=${board.cols})",
                    node.cell.col in 0 until board.cols
                )
            }
        }
    }

    @Test
    fun `la cantidad de colores usados nunca excede la paleta disponible`() {
        for (index in indicesCubriendoLaCurva) {
            val board = generate(seed = 7L, index = index)
            val distinctColors = board.nodes.map { it.color }.distinct()
            assertTrue(
                "index=$index: se usaron ${distinctColors.size} colores, la paleta solo tiene ${PuzzleColor.entries.size}",
                distinctColors.size <= PuzzleColor.entries.size
            )
        }
    }

    @Test
    fun `el mismo seed y el mismo index producen siempre el mismo tablero (determinismo del Reto Diario)`() {
        val boardA = generate(seed = 12345L, index = 10)
        val boardB = generate(seed = 12345L, index = 10)

        assertEquals(boardA, boardB)
    }

    @Test
    fun `seeds distintos con el mismo index normalmente producen tableros distintos`() {
        val boardA = generate(seed = 1L, index = 10)
        val boardB = generate(seed = 2L, index = 10)

        assertTrue(boardA != boardB)
    }
}
