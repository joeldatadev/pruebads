package com.hoshiraflow.domain.model

/**
 * Define el sistema de adyacencia y proyección geométrica del tablero.
 */
enum class BoardTopology {
    /** Grilla 2D estándar con adyacencia ortogonal (4 vecinos). */
    CARTESIAN,

    /**
     * Modelo de cubo real con 3 caras visibles (TOP, LEFT, RIGHT).
     * Cada cara es un grid NxM independiente conectado por aristas.
     */
    CUBE
}
