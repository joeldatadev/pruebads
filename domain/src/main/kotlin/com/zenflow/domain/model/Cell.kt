package com.zenflow.domain.model

/**
 * Ruta destino: :domain/src/main/kotlin/com/zenflow/domain/model/Cell.kt
 */
data class Cell(
    val row: Int,
    val col: Int
) {
    fun isAdjacentTo(other: Cell): Boolean {
        val dRow = kotlin.math.abs(row - other.row)
        val dCol = kotlin.math.abs(col - other.col)
        return (dRow == 1 && dCol == 0) || (dRow == 0 && dCol == 1)
    }
}
