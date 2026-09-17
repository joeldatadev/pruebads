package com.hoshiraflow.domain.usecase

import com.hoshiraflow.domain.model.Board
import com.hoshiraflow.domain.model.CellType
import com.hoshiraflow.domain.model.PuzzleColor

/**
 * Ruta destino: :domain/src/main/kotlin/com/zenflow/domain/usecase/CheckLevelCompleteUseCase.kt
 */
class CheckLevelCompleteUseCase {

    /** true si el color conecta sus dos nodos extremos. Dispara el ParticleSystem en presentation. */
    fun isColorConnected(board: Board, color: PuzzleColor): Boolean {
        val nodes = board.nodesOf(color)
        if (nodes.size != 2) return false
        val path = board.paths[color].orEmpty()
        if (path.isEmpty()) return false
        val (start, end) = nodes
        return (path.first() == start.cell && path.last() == end.cell) ||
            (path.first() == end.cell && path.last() == start.cell)
    }

    /** true si TODOS los colores están conectados Y el tablero está 100% relleno (excluyendo celdas bloqueadas y vacias). */
    fun isLevelComplete(board: Board): Boolean {
        val allColors = board.paths.keys
        if (allColors.isEmpty()) return false
        
        val allConnected = allColors.all { isColorConnected(board, it) }
        if (!allConnected) return false
        
        // Final sanity check: Ensure ALL nodes in the board are covered by their respective paths
        val allNodesConnected = board.nodes.all { node ->
            val path = board.paths[node.color].orEmpty()
            node.cell in path
        }
        if (!allNodesConnected) return false

        val nonPlayableCount = board.cellTypes.values.count { it == CellType.Blocked || it == CellType.Void }
        val requiredCells = board.totalCells() - nonPlayableCount
        val fullyFilled = board.filledCells() == requiredCells

        return fullyFilled
    }
}
