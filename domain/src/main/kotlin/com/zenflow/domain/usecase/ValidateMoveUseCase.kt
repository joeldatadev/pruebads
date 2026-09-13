package com.zenflow.domain.usecase

import com.zenflow.domain.model.Board
import com.zenflow.domain.model.Cell
import com.zenflow.domain.model.PuzzleColor

class ValidateMoveUseCase {

    sealed class Result {
        data class Extend(val newPath: List<Cell>) : Result()
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

        if (!lastCell.isAdjacentTo(targetCell)) return Result.Invalid

        val occupiedBy = board.isCellOccupiedByAnyColor(targetCell)
        if (occupiedBy != null && occupiedBy != color) return Result.Invalid

        val nodeAtTarget = board.nodes.firstOrNull { it.cell == targetCell }
        if (nodeAtTarget != null && nodeAtTarget.color != color) return Result.Invalid

        val nodesForColor = board.nodes.filter { it.color == color }
        val alreadyComplete = nodesForColor.size == 2 && nodesForColor.all { it.cell in currentPath }
        if (alreadyComplete) return Result.Invalid

        return Result.Extend(currentPath + targetCell)
    }
}