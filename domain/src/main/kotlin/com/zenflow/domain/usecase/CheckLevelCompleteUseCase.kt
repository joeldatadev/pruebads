package com.zenflow.domain.usecase

import com.zenflow.domain.model.Board
import com.zenflow.domain.model.PuzzleColor

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
        return path.first() == start.cell && path.last() == end.cell ||
            path.first() == end.cell && path.last() == start.cell
    }

    /** true si TODOS los colores están conectados Y el tablero está 100% relleno. */
    fun isLevelComplete(board: Board): Boolean {
        val allColors = board.nodes.map { it.color }.distinct()
        val allConnected = allColors.all { isColorConnected(board, it) }
        val fullyFilled = board.filledCells() == board.totalCells()
        return allConnected && fullyFilled
    }
}
