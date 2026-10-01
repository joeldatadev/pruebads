package com.hoshiraflow.domain.model

/**
 * Define el sistema de adyacencia y proyección geométrica del tablero.
 */
enum class BoardTopology {
    /** Grilla 2D estándar con adyacencia ortogonal (4 vecinos). */
    CARTESIAN,

    /**
     * Modelo de cubo real con 3 caras visibles (TOP, LEFT, RIGHT), opcionalmente
     * repetido en varios bloques encadenados en escalera (ver [Board.cubeBlocks]).
     */
    CUBE,

    /**
     * Superficie arbitraria hecha de voxels (pirámide, escalera, zigzag, torre...).
     * La adyacencia sale de [SurfaceModel.neighbors], calculada geométricamente
     * a partir de las caras visibles reales, no de una tabla fija.
     */
    SURFACE
}
