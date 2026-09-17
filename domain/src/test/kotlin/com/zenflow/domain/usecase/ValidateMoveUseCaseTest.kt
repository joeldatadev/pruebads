package com.hoshiraflow.domain.usecase

/**
 * Ruta destino: domain/src/test/kotlin/com/zenflow/domain/usecase/ValidateMoveUseCaseTest.kt
 */
import com.hoshiraflow.domain.model.Board
import com.hoshiraflow.domain.model.Cell
import com.hoshiraflow.domain.model.Node
import com.hoshiraflow.domain.model.PuzzleColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidateMoveUseCaseTest {

    private val validateMove = ValidateMoveUseCase()

    /** Tablero 3x3: RED de (0,0) a (2,2), BLUE de (0,2) a (2,0). */
    private fun testBoard(paths: Map<PuzzleColor, List<Cell>> = emptyMap()): Board = Board(
        rows = 3,
        cols = 3,
        nodes = listOf(
            Node(Cell(0, 0), PuzzleColor.RED),
            Node(Cell(2, 2), PuzzleColor.RED),
            Node(Cell(0, 2), PuzzleColor.BLUE),
            Node(Cell(2, 0), PuzzleColor.BLUE)
        ),
        paths = paths
    )

    @Test
    fun `extiende cuando la celda es adyacente y esta libre`() {
        val board = testBoard(paths = mapOf(PuzzleColor.RED to listOf(Cell(0, 0))))
        val result = validateMove(board, PuzzleColor.RED, Cell(0, 1))

        assertTrue(result is ValidateMoveUseCase.Result.Extend)
        assertEquals(listOf(Cell(0, 0), Cell(0, 1)), (result as ValidateMoveUseCase.Result.Extend).newPath)
    }

    @Test
    fun `invalido cuando la celda no es adyacente (salto)`() {
        val board = testBoard(paths = mapOf(PuzzleColor.RED to listOf(Cell(0, 0))))
        val result = validateMove(board, PuzzleColor.RED, Cell(0, 2))

        assertEquals(ValidateMoveUseCase.Result.Invalid, result)
    }

    @Test
    fun `invalido cuando la celda esta ocupada por otro color`() {
        val board = testBoard(
            paths = mapOf(
                PuzzleColor.RED to listOf(Cell(0, 0)),
                PuzzleColor.BLUE to listOf(Cell(0, 2), Cell(0, 1))
            )
        )
        val result = validateMove(board, PuzzleColor.RED, Cell(0, 1))

        assertEquals(ValidateMoveUseCase.Result.Invalid, result)
    }

    @Test
    fun `invalido al tocar un nodo de otro color`() {
        val board = testBoard(paths = mapOf(PuzzleColor.RED to listOf(Cell(0, 0), Cell(0, 1))))
        val result = validateMove(board, PuzzleColor.RED, Cell(0, 2)) // nodo BLUE

        assertEquals(ValidateMoveUseCase.Result.Invalid, result)
    }

    @Test
    fun `retrocede cuando la celda ya es parte del propio path`() {
        val board = testBoard(
            paths = mapOf(PuzzleColor.RED to listOf(Cell(0, 0), Cell(0, 1), Cell(1, 1)))
        )
        val result = validateMove(board, PuzzleColor.RED, Cell(0, 0))

        assertTrue(result is ValidateMoveUseCase.Result.Retreat)
        assertEquals(listOf(Cell(0, 0)), (result as ValidateMoveUseCase.Result.Retreat).newPath)
    }

    @Test
    fun `invalido si el color ya conecto sus dos nodos y se intenta seguir extendiendo`() {
        // RED ya llegó de (0,0) a (2,2) pasando por una ruta válida
        val completePath = listOf(
            Cell(0, 0), Cell(1, 0), Cell(2, 0), Cell(2, 1), Cell(2, 2)
        )
        val board = testBoard(paths = mapOf(PuzzleColor.RED to completePath))
        val result = validateMove(board, PuzzleColor.RED, Cell(1, 2))

        assertEquals(ValidateMoveUseCase.Result.Invalid, result)
    }

    @Test
    fun `invalido si el path esta vacio (no se inicio con onNodeTouched)`() {
        val board = testBoard()
        val result = validateMove(board, PuzzleColor.RED, Cell(0, 1))

        assertEquals(ValidateMoveUseCase.Result.Invalid, result)
    }

    @Test
    fun `invalido si la celda es de tipo Void`() {
        val board = testBoard(paths = mapOf(PuzzleColor.RED to listOf(Cell(0, 0))))
            .copy(cellTypes = mapOf(Cell(0, 1) to com.hoshiraflow.domain.model.CellType.Void))
        val result = validateMove(board, PuzzleColor.RED, Cell(0, 1))

        assertEquals(ValidateMoveUseCase.Result.Invalid, result)
    }
}



