package com.hoshiraflow.domain.usecase

import com.hoshiraflow.domain.model.Board
import com.hoshiraflow.domain.model.BoardTopology
import com.hoshiraflow.domain.model.Cell
import com.hoshiraflow.domain.model.CellType
import com.hoshiraflow.domain.model.PuzzleColor
import com.hoshiraflow.domain.util.CubeEdgeMap

class ValidateMoveUseCase {

    sealed class Result {
        data class Extend(val newPath: List<Cell>) : Result()
        data class MutateColor(val newPath: List<Cell>, val newColor: PuzzleColor) : Result()
        data class Retreat(val newPath: List<Cell>) : Result()
        object Invalid : Result()
    }

    operator fun invoke(board: Board, color: PuzzleColor, targetCell: Cell): Result {
        val currentPath = board.paths[color].orEmpty()
        if (currentPath.isEmpty()) return Result.Invalid

        val lastCell = currentPath.last()

        val indexInPath = currentPath.indexOf(targetCell)
        if (indexInPath != -1) {
            val trimmed = currentPath.subList(0, indexInPath + 1)
            return Result.Retreat(trimmed)
        }

        val isAdjacent = when (board.topology) {
            BoardTopology.ISOMETRIC -> lastCell.isIsometricAdjacentTo(targetCell)
            BoardTopology.CUBE -> CubeEdgeMap.areAdjacent(lastCell, targetCell, board.rows)
                || lastCell.isAdjacentTo(targetCell)
            else -> lastCell.isAdjacentTo(targetCell)
        }

        if (!isAdjacent) return Result.Invalid

        // Reject move immediately if target cell is CellType.Void (black masked areas)
        if (board.cellTypes[targetCell] == CellType.Void) return Result.Invalid

        val occupiedBy = board.isCellOccupiedByAnyColor(targetCell)
        if (occupiedBy != null && occupiedBy != color) return Result.Invalid

        val nodeAtTarget = board.nodes.firstOrNull { it.cell == targetCell }
        if (nodeAtTarget != null) {
            // Find pair ID of the current path's starting cell to ensure matching identity
            val startCell = currentPath.first()
            val startNode = board.nodes.firstOrNull { it.cell == startCell }
            if (nodeAtTarget.pairId != startNode?.pairId) return Result.Invalid
        }

        val nodesForColor = board.nodes.filter { it.color == color }
        val alreadyComplete = nodesForColor.size == 2 && nodesForColor.all { it.cell in currentPath }
        if (alreadyComplete) return Result.Invalid

        // Lógica de Portales (Bidirectional & Symmetric)
        val cellType = board.cellTypes[targetCell]
        if (cellType is CellType.Portal) {
            val destination = board.cellTypes.entries.firstOrNull { 
                val type = it.value
                type is CellType.Portal && type.portalId == cellType.portalId && it.key != targetCell
            }?.key ?: return Result.Invalid

            // Verificar si la salida está ocupada por otro color
            val occupiedAtDest = board.isCellOccupiedByAnyColor(destination)
            if (occupiedAtDest != null && occupiedAtDest != color) return Result.Invalid

            // Verificar si hay un nodo de otro color en la salida
            val nodeAtDest = board.nodes.firstOrNull { it.cell == destination }
            if (nodeAtDest != null && nodeAtDest.color != color) return Result.Invalid

            // La conexión de portales es bidireccional y simétrica: A -> B y B -> A funcionan igual
            return Result.Extend(currentPath + targetCell + destination)
        }

        // Lógica de Interruptores de Color (Switch)
        if (cellType is CellType.Switch) {
            // El camino se extiende a la casilla del interruptor y notifica el cambio de color
            return Result.MutateColor(currentPath + targetCell, cellType.targetColor)
        }

        return Result.Extend(currentPath + targetCell)
    }
}


