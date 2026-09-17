package com.hoshiraflow.domain.model

import androidx.compose.runtime.Immutable

/**
 * Ruta destino: :domain/src/main/kotlin/com/zenflow/domain/model/Board.kt
 *
 * Estado inmutable del tablero. Cada cambio genera una nueva instancia
 * (copy()) para que StateFlow en el ViewModel detecte el cambio limpiamente.
 */
@Immutable
data class Board(
    val rows: Int = 5,
    val cols: Int = 5,
    val width: Int = 5,
    val height: Int = 5,
    val nodes: List<Node> = emptyList(),
    // paths: mapa color -> lista ordenada de celdas que forman la línea actual
    val paths: Map<PuzzleColor, List<Cell>> = emptyMap(),
    val cellTypes: Map<Cell, CellType> = emptyMap(),
    val topology: BoardTopology = BoardTopology.CARTESIAN
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
        
    /** Returns only playable cells (excluding Void and Blocked) */
    fun getPlayableCells(): Set<Cell> {
        val playable = mutableSetOf<Cell>()
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                // In 3D we check all Z layers stored in cellTypes
                cellTypes.keys.filter { it.row == r && it.col == c }.forEach { cell ->
                    val type = cellTypes[cell]
                    if (type != CellType.Void && type != CellType.Blocked) {
                        playable.add(cell)
                    }
                }
            }
        }
        return playable
    }
}
