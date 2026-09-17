package com.hoshiraflow.domain.model

import androidx.compose.runtime.Immutable

/**
 * Ruta destino: :domain/src/main/kotlin/com/zenflow/domain/model/Node.kt
 */
@Immutable
data class Node(
    val cell: Cell,
    val color: PuzzleColor,
    val pairId: Int = 0
)



