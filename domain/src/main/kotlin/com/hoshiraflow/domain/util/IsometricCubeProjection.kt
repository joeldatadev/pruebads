package com.hoshiraflow.domain.util

import com.hoshiraflow.domain.model.CubeFace

/**
 * Utilidad para proyectar celdas de un cubo de 3 caras (TOP, LEFT, RIGHT)
 * a coordenadas de pantalla (Pair<Float, Float>).
 * 
 * Basado en una proyección isométrica donde las 3 caras se encuentran en un vértice central.
 */
object IsometricCubeProjection {

    private const val COS_30 = 0.8660254f
    private const val SIN_30 = 0.5f

    /**
     * Calcula el centro de una celda en pantalla.
     * @param face Cara del cubo
     * @param u Columna local (0..N-1)
     * @param v Fila local (0..N-1)
     * @param cellSize Tamaño base de la celda
     * @param originX Centro X de la proyección (vértice donde se unen las 3 caras)
     * @param originY Centro Y de la proyección
     */
    fun cellToScreen(
        face: CubeFace,
        u: Int,
        v: Int,
        cellSize: Float,
        originX: Float,
        originY: Float
    ): Pair<Float, Float> {
        val w = cellSize * COS_30
        val h = cellSize * SIN_30

        return when (face) {
            CubeFace.TOP -> {
                // u aumenta hacia atrás-izquierda, v hacia atrás-derecha
                val x = originX + (v - u) * w
                val y = originY - (u + v + 1) * h
                x to y
            }
            CubeFace.LEFT -> {
                // u aumenta hacia atrás-izquierda (comparte con TOP), v hacia abajo
                val x = originX - (u + 0.5f) * w
                val y = originY - (u + 0.5f) * h + (v + 0.5f) * cellSize
                x to y
            }
            CubeFace.RIGHT -> {
                // u aumenta hacia atrás-derecha (comparte con TOP), v hacia abajo
                val x = originX + (u + 0.5f) * w
                val y = originY - (u + 0.5f) * h + (v + 0.5f) * cellSize
                x to y
            }
        }
    }

    /**
     * Devuelve los 4 vértices que forman el rombo de la celda para dibujar el fondo.
     */
    fun getCellPolygon(
        face: CubeFace,
        u: Int,
        v: Int,
        cellSize: Float,
        originX: Float,
        originY: Float
    ): List<Pair<Float, Float>> {
        val w = cellSize * COS_30
        val h = cellSize * SIN_30
        
        return when (face) {
            CubeFace.TOP -> {
                val cx = originX + (v - u) * w
                val cy = originY - (u + v + 1) * h
                listOf(
                    cx to (cy - h),
                    (cx + w) to cy,
                    cx to (cy + h),
                    (cx - w) to cy
                )
            }
            CubeFace.LEFT -> {
                val x = originX - u * w
                val y = originY - u * h + v * cellSize
                listOf(
                    x to y,
                    (x - w) to (y - h),
                    (x - w) to (y - h + cellSize),
                    x to (y + cellSize)
                )
            }
            CubeFace.RIGHT -> {
                val x = originX + u * w
                val y = originY - u * h + v * cellSize
                listOf(
                    x to y,
                    (x + w) to (y - h),
                    (x + w) to (y - h + cellSize),
                    x to (y + cellSize)
                )
            }
        }
    }
}
