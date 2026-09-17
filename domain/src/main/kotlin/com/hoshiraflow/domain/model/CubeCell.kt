package com.hoshiraflow.domain.model

import androidx.compose.runtime.Immutable

/**
 * Coordenada dentro de una cara específica del cubo.
 * (u, v) son las coordenadas locales (columna, fila) dentro de esa cara.
 */
@Immutable
data class CubeCell(
    val face: CubeFace,
    val u: Int,
    val v: Int
)
