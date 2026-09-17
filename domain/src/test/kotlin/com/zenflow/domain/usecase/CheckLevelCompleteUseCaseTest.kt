package com.hoshiraflow.domain.usecase

/**
 * Ruta destino: domain/src/test/kotlin/com/zenflow/domain/usecase/CheckLevelCompleteUseCaseTest.kt
 */
import com.hoshiraflow.domain.model.Board
import com.hoshiraflow.domain.model.Cell
import com.hoshiraflow.domain.model.Node
import com.hoshiraflow.domain.model.PuzzleColor
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckLevelCompleteUseCaseTest {

    private val checkComplete = CheckLevelCompleteUseCase()

    // Tablero 2x2: RED conecta (0,0)-(1,1), BLUE conecta (0,1)-(1,0)
    private fun board(paths: Map<PuzzleColor, List<Cell>>): Board = Board(
        rows = 2,
        cols = 2,
        nodes = listOf(
            Node(Cell(0, 0), PuzzleColor.RED),
            Node(Cell(1, 1), PuzzleColor.RED),
            Node(Cell(0, 1), PuzzleColor.BLUE),
            Node(Cell(1, 0), PuzzleColor.BLUE)
        ),
        paths = paths
    )

    @Test
    fun `color conectado cuando el path va de un extremo al otro`() {
        val b = board(mapOf(PuzzleColor.RED to listOf(Cell(0, 0), Cell(0, 1), Cell(1, 1))))
        assertTrue(checkComplete.isColorConnected(b, PuzzleColor.RED))
    }

    @Test
    fun `color NO conectado si el path no llega al segundo nodo`() {
        val b = board(mapOf(PuzzleColor.RED to listOf(Cell(0, 0), Cell(0, 1))))
        assertFalse(checkComplete.isColorConnected(b, PuzzleColor.RED))
    }

    @Test
    fun `nivel NO completo si todos los colores conectan pero quedan casillas vacias`() {
        // RED y BLUE conectados directo (2 celdas cada uno), pero el tablero
        // 2x2 tiene 4 celdas - acá solo se llenan las 4 (caso borde), así que
        // probamos con una versión donde deliberadamente sobra una celda vacía
        // usando un tablero 2x3 (6 celdas) donde los paths solo cubren 4.
        val b6 = Board(
            rows = 2,
            cols = 3,
            nodes = listOf(
                Node(Cell(0, 0), PuzzleColor.RED),
                Node(Cell(0, 1), PuzzleColor.RED),
                Node(Cell(1, 0), PuzzleColor.BLUE),
                Node(Cell(1, 1), PuzzleColor.BLUE)
            ),
            paths = mapOf(
                PuzzleColor.RED to listOf(Cell(0, 0), Cell(0, 1)),
                PuzzleColor.BLUE to listOf(Cell(1, 0), Cell(1, 1))
            )
        )

        assertTrue(checkComplete.isColorConnected(b6, PuzzleColor.RED))
        assertTrue(checkComplete.isColorConnected(b6, PuzzleColor.BLUE))
        // Regla de negocio explícita del proyecto: conectado != completo si
        // sobran casillas vacías (columnas 2 de cada fila nunca se llenaron).
        assertFalse(checkComplete.isLevelComplete(b6))
    }

    @Test
    fun `nivel completo cuando todos los colores conectan Y el tablero esta 100% lleno`() {
        // Tablero 2x2 simple y realmente solucionable: RED ocupa la fila de
        // arriba, BLUE la de abajo - sin pisarse entre colores.
        val b = Board(
            rows = 2,
            cols = 2,
            nodes = listOf(
                Node(Cell(0, 0), PuzzleColor.RED),
                Node(Cell(0, 1), PuzzleColor.RED),
                Node(Cell(1, 0), PuzzleColor.BLUE),
                Node(Cell(1, 1), PuzzleColor.BLUE)
            ),
            paths = mapOf(
                PuzzleColor.RED to listOf(Cell(0, 0), Cell(0, 1)),
                PuzzleColor.BLUE to listOf(Cell(1, 0), Cell(1, 1))
            )
        )
        assertTrue(checkComplete.isLevelComplete(b))
    }
}



