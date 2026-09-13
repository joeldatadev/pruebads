package com.zenflow.app.game

import kotlin.math.abs

class BoardController(
    initialBoard: Board
) {
    var board = initialBoard
        private set

    private var activeColor: GameColor? = null

    fun onTouchStart(cell: Cell) {
        val node = board.nodes.firstOrNull { it.cell == cell } ?: return
        activeColor = node.color

        // Iniciar nuevo camino desde la celda tocada
        val updatedPaths = board.paths.toMutableMap()
        updatedPaths[node.color] = listOf(cell)
        board = board.copy(paths = updatedPaths)
    }

    fun onTouchMove(cell: Cell) {
        val color = activeColor ?: return
        val currentPath = board.paths[color] ?: return
        if (currentPath.isEmpty()) return

        val lastCell = currentPath.last()

        // 1. Si se mueve a la misma celda donde ya está, se ignora
        if (lastCell == cell) return

        // 2. Si retrocede a la celda anterior en el camino, se recorta para borrar fluido
        if (currentPath.size >= 2 && currentPath[currentPath.size - 2] == cell) {
            val updatedPaths = board.paths.toMutableMap()
            updatedPaths[color] = currentPath.dropLast(1)
            board = board.copy(paths = updatedPaths)
            return
        }

        // 3. Validar movimiento adyacente (horizontal o vertical de 1 paso)
        val isAdjacent = (abs(lastCell.row - cell.row) + abs(lastCell.col - cell.col)) == 1

        if (isAdjacent && !currentPath.contains(cell)) {
            // Verificar si choca con un nodo de OTRO color
            val destinationNode = board.nodes.firstOrNull { it.cell == cell }
            if (destinationNode != null && destinationNode.color != color) {
                return
            }

            val updatedPaths = board.paths.toMutableMap()
            updatedPaths[color] = currentPath + cell
            board = board.copy(paths = updatedPaths)
        }
    }

    fun onTouchEnd() {
        val color = activeColor ?: return
        val currentPath = board.paths[color] ?: emptyList()
        val nodesOfColor = board.nodes.filter { it.color == color }

        // Si el camino no une los 2 nodos del mismo color, se limpia
        val startsAtNode = nodesOfColor.any { it.cell == currentPath.firstOrNull() }
        val endsAtNode = nodesOfColor.any { it.cell == currentPath.lastOrNull() }
        val isComplete = currentPath.size > 1 && startsAtNode && endsAtNode && currentPath.first() != currentPath.last()

        if (!isComplete) {
            val updatedPaths = board.paths.toMutableMap()
            updatedPaths.remove(color)
            board = board.copy(paths = updatedPaths)
        }

        activeColor = null
    }
}