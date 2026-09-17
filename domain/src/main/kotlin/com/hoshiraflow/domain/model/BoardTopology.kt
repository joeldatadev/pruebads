package com.hoshiraflow.domain.model

/**
 * Define el sistema de adyacencia y proyección geométrica del tablero.
 */
enum class BoardTopology {
    /** Grilla 2D estándar con adyacencia ortogonal (4 vecinos). */
    CARTESIAN,

    /** 
     * Proyección isométrica 3D basada en rombos cuadrados.
     * Usa adyacencia ortogonal 3D (6 vecinos: N, S, E, W + Arriba, Abajo).
     * Referencia: ver IsometricProjection.kt para el renderizado.
     * Nota: Futuras topologías hexagonales (tipo panal) deben usar un nuevo enum (ej. HEX_ISOMETRIC)
     * y su propia fórmula de proyección, no reutilizar la lógica de ISOMETRIC.
     */
    ISOMETRIC
}
