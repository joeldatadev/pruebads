package com.zenflow.domain.model

/**
 * Ruta destino: :domain/src/main/kotlin/com/zenflow/domain/model/Board.kt
 *
 * Estado inmutable del tablero. Cada cambio genera una nueva instancia
 * (copy()) para que StateFlow en el ViewModel detecte el cambio limpiamente.
 */
data class Board(
    val rows: Int,
    val cols: Int,
    val nodes: List<Node>,
    // paths: mapa color -> lista ordenada de celdas que forman la línea actual
    val paths: Map<PuzzleColor, List<Cell>> = emptyMap()
) {
    fun nodesOf(color: PuzzleColor): List<Node> = nodes.filter { it.color == color }

    fun isCellOccupiedBy(cell: Cell, color: PuzzleColor): Boolean =
        paths[color]?.contains(cell) == true

    fun isCellOccupiedByAnyColor(cell: Cell): PuzzleColor? =
        paths.entries.firstOrNull { (_, cells) -> cell in cells }?.key

    fun totalCells(): Int = rows * cols

    fun filledCells(): Int = paths.values.sumOf { it.size }

    fun withPath(color: PuzzleColor, path: List<Cell>): Board =
        copy(paths = paths.toMutableMap().apply { put(color, path) })

    fun clearPath(color: PuzzleColor): Board =
        copy(paths = paths.toMutableMap().apply { remove(color) })
}
