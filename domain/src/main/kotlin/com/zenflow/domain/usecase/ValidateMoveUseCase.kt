package com.zenflow.domain.usecase

import com.zenflow.domain.model.Board
import com.zenflow.domain.model.Cell
import com.zenflow.domain.model.PuzzleColor

/**
 * Ruta destino: :domain/src/main/kotlin/com/zenflow/domain/usecase/ValidateMoveUseCase.kt
 *
 * Reglas cubiertas:
 * 1. La celda destino debe ser adyacente a la última celda del path actual.
 * 2. No se puede pisar una celda ocupada por OTRO color.
 * 3. Si la celda ya es parte del propio path (retroceso), se recorta el path
 *    hasta ese punto (comportamiento estilo Flow Free).
 * 4. No se permite cerrar el path sobre sí mismo salvo que sea el nodo final
 *    del mismo color.
 */
class ValidateMoveUseCase {

    sealed class Result {
        data class Extend(val newPath: List<Cell>) : Result()
        data class Retreat(val newPath: List<Cell>) : Result()
        object Invalid : Result()
    }

    operator fun invoke(board: Board, color: PuzzleColor, targetCell: Cell): Result {
        val currentPath = board.paths[color].orEmpty()
        if (currentPath.isEmpty()) return Result.Invalid // debe iniciarse con onNodeTouched, no aquí

        val lastCell = currentPath.last()

        // Retroceso: el usuario volvió a pasar por una celda que ya es parte de su propia línea
        val indexInPath = currentPath.indexOf(targetCell)
        if (indexInPath != -1) {
            val trimmed = currentPath.subList(0, indexInPath + 1)
            return Result.Retreat(trimmed)
        }

        // Debe ser adyacente a la última celda
        if (!lastCell.isAdjacentTo(targetCell)) return Result.Invalid

        // No se puede pisar celda ocupada por otro color
        val occupiedBy = board.isCellOccupiedByAnyColor(targetCell)
        if (occupiedBy != null && occupiedBy != color) return Result.Invalid

        // Si la celda destino es un nodo de OTRO color, inválido
        val nodeAtTarget = board.nodes.firstOrNull { it.cell == targetCell }
        if (nodeAtTarget != null && nodeAtTarget.color != color) return Result.Invalid

        // Si la celda destino es un nodo del MISMO color pero no es el nodo opuesto
        // al de inicio, igual es válido (podría ser el punto de partida repetido).
        return Result.Extend(currentPath + targetCell)
    }
}
