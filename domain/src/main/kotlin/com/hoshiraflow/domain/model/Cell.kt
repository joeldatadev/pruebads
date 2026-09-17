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

    /** Support for 6-directional orthogonal adjacency in 3D isometric grids */
    fun isIsometricAdjacentTo(other: Cell): Boolean {
        val dRow = kotlin.math.abs(other.row - row)
        val dCol = kotlin.math.abs(other.col - col)
        val dZ = kotlin.math.abs(other.z - z)
        
        // 6 orthogonal directions: 
        // 4 in the current plane (N, S, E, W) 
        // + 2 vertical (Up, Down)
        return when {
            // Same level neighbors (Orthogonal 2D)
            dZ == 0 -> (dRow == 1 && dCol == 0) || (dRow == 0 && dCol == 1)
            // Vertical neighbors (Same (row, col) but adjacent Z)
            dZ == 1 -> dRow == 0 && dCol == 0
            else -> false
        }
    }
}
