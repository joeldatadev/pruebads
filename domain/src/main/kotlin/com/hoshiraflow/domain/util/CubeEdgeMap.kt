package com.hoshiraflow.domain.util

import com.hoshiraflow.domain.model.Cell
import com.hoshiraflow.domain.model.CubeFace

/**
 * Mapeo de adyacencias entre las aristas de un cubo de 3 caras (TOP, LEFT, RIGHT).
 *
 * Verificado geométricamente (no derivado a mano): dos celdas de caras
 * distintas son "vecinas de cubo" si sus polígonos (ver [CubeProjection.getCellPolygon])
 * comparten un borde completo. Comprobando eso para las 3 caras se obtiene
 * exactamente:
 *
 *   TOP(u, 0)  <-> LEFT(u, 0)     para todo u
 *   TOP(0, v)  <-> RIGHT(v, 0)    para todo v
 *   LEFT(0, v) <-> RIGHT(0, v)    para todo v
 *
 * FIX: la versión anterior usaba `u == n - 1` como borde TOP<->LEFT. Ese es
 * en realidad un borde EXTERIOR de la cara TOP (silueta del cubo, sin
 * vecino), mientras que el borde real (`v == 0`) no tenía mapeo alguno:
 *   - T(1,0), T(2,0) quedaban sin vecino -> `crossEdge` devolvía null ->
 *     bloqueo total del trazo al intentar cruzar.
 *   - T(3,0) cruzaba a L(0,0) en vez de a L(3,0): transición a la celda
 *     equivocada.
 *   - La esquina donde se tocan las 3 caras (T(0,0)/L(0,0)/R(0,0)) tiene DOS
 *     aristas compartidas, pero `Neighbor?` solo podía representar una.
 */
object CubeEdgeMap {

    data class Neighbor(val face: CubeFace, val u: Int, val v: Int)

    /**
     * Todos los vecinos de (face, u, v) al cruzar una arista compartida del
     * cubo. Normalmente devuelve 0 o 1 elementos; en la celda de esquina
     * donde se unen las 3 caras devuelve 2 (una arista con cada otra cara).
     */
    fun crossEdges(face: CubeFace, u: Int, v: Int, n: Int): List<Neighbor> {
        val neighbors = mutableListOf<Neighbor>()
        when (face) {
            CubeFace.TOP -> {
                if (v == 0) neighbors.add(Neighbor(CubeFace.LEFT, u, 0))
                if (u == 0) neighbors.add(Neighbor(CubeFace.RIGHT, v, 0))
            }
            CubeFace.LEFT -> {
                if (v == 0) neighbors.add(Neighbor(CubeFace.TOP, u, 0))
                if (u == 0) neighbors.add(Neighbor(CubeFace.RIGHT, 0, v))
            }
            CubeFace.RIGHT -> {
                if (v == 0) neighbors.add(Neighbor(CubeFace.TOP, 0, u))
                if (u == 0) neighbors.add(Neighbor(CubeFace.LEFT, 0, v))
            }
        }
        return neighbors
    }

    /**
     * Compat: primer vecino, si existe. Preferir [crossEdges] en cualquier
     * lugar donde la celda pueda ser la esquina triple (tiene 2 vecinos).
     */
    @Deprecated("Usar crossEdges: la esquina triple del cubo tiene 2 vecinos, no 1.", ReplaceWith("crossEdges(face, u, v, n).firstOrNull()"))
    fun crossEdge(face: CubeFace, u: Int, v: Int, n: Int): Neighbor? =
        crossEdges(face, u, v, n).firstOrNull()

    fun zOf(block: Int, face: CubeFace): Int = block * 3 + face.ordinal

    /** Todos los vecinos de una celda (mismo bloque + unión en escalera entre bloques). */
    fun neighborsOf(cell: Cell, n: Int, blocks: Int = 1): List<Cell> {
        val block = cell.z / 3
        if (block !in 0 until blocks) return emptyList()
        val face = CubeFace.entries[cell.z % 3]

        val result = crossEdges(face, cell.row, cell.col, n)
            .map { Cell(it.u, it.v, zOf(block, it.face)) }
            .toMutableList()

        // RIGHT del bloque k -> TOP del bloque k+1
        if (block + 1 < blocks && face == CubeFace.RIGHT && cell.col == n - 1) {
            result += Cell(n - 1, cell.row, zOf(block + 1, CubeFace.TOP))
        }
        // TOP del bloque k -> RIGHT del bloque k-1 (inversa)
        if (block > 0 && face == CubeFace.TOP && cell.row == n - 1) {
            result += Cell(cell.col, n - 1, zOf(block - 1, CubeFace.RIGHT))
        }
        return result
    }

    fun areAdjacent(a: Cell, b: Cell, n: Int, blocks: Int = 1): Boolean =
        b in neighborsOf(a, n, blocks)
}