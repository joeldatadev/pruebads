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
    val nodes: List<Node> = emptyList(),
    // paths: mapa color -> lista ordenada de celdas que forman la línea actual
    val paths: Map<PuzzleColor, List<Cell>> = emptyMap(),
    val cellTypes: Map<Cell, CellType> = emptyMap(),
    val topology: BoardTopology = BoardTopology.CARTESIAN
) {

    /**
     * FIX: width/height dejan de ser campos del constructor con su propio
     * default (5). Antes estaban desconectados de rows/cols y NINGÚN
     * generador los seteaba explícitamente, así que siempre valían 5 sin
     * importar el tamaño real del tablero. GameScreen usa board.width /
     * board.height -no rows/cols- tanto para el tamaño de celda en pantalla
     * como para el bound-check del hit-test táctil (`col !in 0 until
     * board.width`), así que en cualquier tablero != 5x5 (6x6, 7x7, el cubo
     * de n=4, SQUARE(10)...) las filas/columnas con índice >= 5 quedaban sin
     * dibujar y, sobre todo, rechazadas por el gesto: casillas jugables pero
     * "inalcanzables". Al derivarlos siempre quedan sincronizados.
     */
    val width: Int get() = cols
    val height: Int get() = rows

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