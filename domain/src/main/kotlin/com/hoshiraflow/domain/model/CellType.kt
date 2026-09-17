package com.hoshiraflow.domain.model

import androidx.compose.runtime.Immutable

/**
 * Define los tipos de celdas especiales en el tablero.
 */
@Immutable
sealed class CellType {
    /** Celda normal donde se pueden dibujar caminos. */
    object Empty : CellType()

    /** Celda bloqueada (muro) que impide el paso de cualquier camino. */
    object Blocked : CellType()

    /** Celda fuera de los límites de la forma del nivel (no jugable). */
    data object Void : CellType()

    /** Portal que transporta el camino a otra celda con el mismo portalId. */
    data class Portal(val portalId: Int) : CellType()

    /** Interruptor que cambia el color del camino activo al targetColor. */
    data class Switch(val targetColor: PuzzleColor) : CellType()
}
