package com.hoshiraflow.domain.model

import androidx.compose.runtime.Immutable

/**
 * Ruta destino: :domain/src/main/kotlin/com/zenflow/domain/model/Cell.kt
 */
@Immutable
data class Cell(
    val row: Int,
    val col: Int,
    val z: Int = 0
) {
    fun isAdjacentTo(other: Cell): Boolean {
        if (z != other.z) return false
        val dRow = kotlin.math.abs(row - other.row)
        val dCol = kotlin.math.abs(col - other.col)
        return (dRow == 1 && dCol == 0) || (dRow == 0 && dCol == 1)
    }
}
