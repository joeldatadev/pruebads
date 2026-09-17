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

    /** Resultado de encajar un cubo de n x n x n dentro de un área disponible. */
    data class CubeLayout(val originX: Float, val originY: Float, val cellSize: Float)

    /**
     * Calcula origen (vértice donde se unen las 3 caras) y cellSize para que
     * el cubo COMPLETO (sus 3 caras) quepa dentro de [availableWidth] x
     * [availableHeight] sin recortar ninguna celda.
     *
     * FIX: antes GameScreen usaba un origen fijo (layoutWidth/2f,
     * layoutHeight/2.5f) sin relación con `n` ni con el cellSize real. Para
     * n=4 eso hacía que celdas de la cara TOP con (u+v) alto -p.ej. el nodo
     * extremo (n-1, 1)- se proyectaran con y < 0: fuera del área dibujada Y
     * fuera del área que recibe gestos (pointerInput solo ve toques dentro
     * de los límites del Composable). Resultado observado: nodo invisible,
     * intocable, y cualquier color cuyo camino pasara por esa zona (el
     * morado, en el diagnóstico) se bloqueaba justo al intentar avanzar
     * hacia ella, mientras otros colores (cian, amarillo) seguían
     * funcionando por no tocar esa esquina.
     *
     * En vez de derivar una fórmula cerrada a mano (frágil ante cambios de
     * la proyección), medimos el bounding box real recorriendo los VÉRTICES
     * de cada celda con cellSize=1, y luego escalamos + centramos.
     */
    fun fitCubeToArea(
        n: Int,
        availableWidth: Float,
        availableHeight: Float,
        marginFraction: Float = 0.08f
    ): CubeLayout {
        if (n <= 0 || availableWidth <= 0f || availableHeight <= 0f) {
            return CubeLayout(availableWidth / 2f, availableHeight / 2f, 0f)
        }

        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE

        for (face in CubeFace.entries) {
            for (u in 0 until n) {
                for (v in 0 until n) {
                    getCellPolygon(face, u, v, cellSize = 1f, originX = 0f, originY = 0f).forEach { (x, y) ->
                        if (x < minX) minX = x
                        if (x > maxX) maxX = x
                        if (y < minY) minY = y
                        if (y > maxY) maxY = y
                    }
                }
            }
        }

        val unitWidth = maxX - minX
        val unitHeight = maxY - minY
        if (unitWidth <= 0f || unitHeight <= 0f) {
            return CubeLayout(availableWidth / 2f, availableHeight / 2f, 0f)
        }

        val margin = (1f - marginFraction).coerceIn(0.5f, 1f)
        val cellSize = kotlin.math.min(
            (availableWidth * margin) / unitWidth,
            (availableHeight * margin) / unitHeight
        )

        val contentWidth = unitWidth * cellSize
        val contentHeight = unitHeight * cellSize
        val originX = (availableWidth - contentWidth) / 2f - minX * cellSize
        val originY = (availableHeight - contentHeight) / 2f - minY * cellSize

        return CubeLayout(originX, originY, cellSize)
    }

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
                val x = originX + (v - u) * w
                val y = originY - (u + v + 1) * h
                x to y
            }
            CubeFace.LEFT -> {
                val x = originX - (u + 0.5f) * w
                val y = originY - (u + 0.5f) * h + (v + 0.5f) * cellSize
                x to y
            }
            CubeFace.RIGHT -> {
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