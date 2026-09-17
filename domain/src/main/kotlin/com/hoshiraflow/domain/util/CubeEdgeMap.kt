package com.hoshiraflow.domain.util

import com.hoshiraflow.domain.model.Cell
import com.hoshiraflow.domain.model.CubeFace

/**
 * Mapeo de adyacencias entre las aristas de un cubo de 3 caras (TOP, LEFT, RIGHT).
 */
object CubeEdgeMap {

    data class Neighbor(val face: CubeFace, val u: Int, val v: Int)

    /**
     * Dado una celda en una arista, devuelve su vecino en la cara adyacente.
     * Mapeo basado en la proyección isométrica de Hoshira Flow:
     * - TOP(n-1, v) <-> LEFT(v, 0)   [Arista Superior-Izquierda]
     * - TOP(0, v)   <-> RIGHT(v, 0)  [Arista Superior-Derecha]
     * - LEFT(0, v)  <-> RIGHT(0, v)  [Arista Central Vertical]
     */
    fun crossEdge(face: CubeFace, u: Int, v: Int, n: Int): Neighbor? {
        return when (face) {
            CubeFace.TOP -> {
                when {
                    u == n - 1 -> Neighbor(CubeFace.LEFT, v, 0)
                    u == 0 -> Neighbor(CubeFace.RIGHT, v, 0)
                    else -> null
                }
            }
            CubeFace.LEFT -> {
                when {
                    v == 0 -> Neighbor(CubeFace.TOP, n - 1, u)
                    u == 0 -> Neighbor(CubeFace.RIGHT, 0, v)
                    else -> null
                }
            }
            CubeFace.RIGHT -> {
                when {
                    v == 0 -> Neighbor(CubeFace.TOP, 0, u)
                    u == 0 -> Neighbor(CubeFace.LEFT, 0, v)
                    else -> null
                }
            }
        }
    }

    /**
     * Verifica si dos celdas son adyacentes a través de una arista del cubo.
     */
    fun areAdjacent(a: Cell, b: Cell, n: Int): Boolean {
        val faceA = try { CubeFace.entries[a.z] } catch (e: Exception) { return false }
        val neighbor = crossEdge(faceA, a.row, a.col, n) ?: return false
        
        return neighbor.face.ordinal == b.z && 
               neighbor.u == b.row && 
               neighbor.v == b.col
    }
}
