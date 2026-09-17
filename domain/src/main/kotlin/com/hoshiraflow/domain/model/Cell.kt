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

    /** Support for 6-directional adjacency in isometric/hexagonal grids */
    fun isIsometricAdjacentTo(other: Cell): Boolean {
        val dRow = other.row - row
        val dCol = other.col - col
        val dZ = other.z - z
        
        // 6 directions for 3D isometric layout (staggered/axial style)
        // Including vertical moves if applicable, but usually 6 neighbors in a flat plane
        // Here we support z transitions if they are 1 step away and row/col are adjacent
        
        return when {
            // Horizontal planes neighbors
            dZ == 0 && dRow == 0 && dCol == 1 -> true   // Right
            dZ == 0 && dRow == 1 && dCol == 0 -> true   // Bottom-Right
            dZ == 0 && dRow == 1 && dCol == -1 -> true  // Bottom-Left
            dZ == 0 && dRow == 0 && dCol == -1 -> true  // Left
            dZ == 0 && dRow == -1 && dCol == 0 -> true  // Top-Left
            dZ == 0 && dRow == -1 && dCol == 1 -> true  // Top-Right
            
            // Volumetric vertical neighbors (up/down)
            kotlin.math.abs(dZ) == 1 && dRow == 0 && dCol == 0 -> true
            
            else -> false
        }
    }
}
