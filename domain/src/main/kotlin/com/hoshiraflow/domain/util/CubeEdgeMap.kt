package com.hoshiraflow.domain.util

import com.hoshiraflow.domain.model.Cell
import com.hoshiraflow.domain.model.CubeCell
import com.hoshiraflow.domain.model.CubeFace

/**
 * Tabla de adyacencia para las aristas compartidas entre las 3 caras del cubo.
 * Define cómo una celda en el borde de una cara se traduce a la celda vecina al cruzar la arista.
 */
object CubeEdgeMap {

    fun getAdjacent(cell: CubeCell, n: Int): List<CubeCell> {
        val u = cell.u
        val v = cell.v
        val face = cell.face
        val adj = mutableListOf<CubeCell>()

        // 1. Adyacencia interna de la cara (4 direcciones)
        if (u > 0) adj.add(cell.copy(u = u - 1))
        if (u < n - 1) adj.add(cell.copy(u = u + 1))
        if (v > 0) adj.add(cell.copy(v = v - 1))
        if (v < n - 1) adj.add(cell.copy(v = v + 1))

        // 2. Adyacencia entre aristas compartidas
        when (face) {
            CubeFace.TOP -> {
                // Arista TOP-LEFT (u=N-1 en TOP conecta con v=0 en LEFT)
                if (u == n - 1) adj.add(CubeCell(CubeFace.LEFT, v, 0))
                
                // Arista TOP-RIGHT (u=0 en TOP conecta con v=0 en RIGHT)
                if (u == 0) adj.add(CubeCell(CubeFace.RIGHT, v, 0))
            }
            CubeFace.LEFT -> {
                // Arista LEFT-TOP (v=0 en LEFT conecta con u=N-1 en TOP)
                if (v == 0) adj.add(CubeCell(CubeFace.TOP, n - 1, u))
                
                // Arista vertical LEFT-RIGHT (u=0 en LEFT conecta con u=0 en RIGHT)
                if (u == 0) adj.add(CubeCell(CubeFace.RIGHT, 0, v))
                
                // Arista base LEFT-RIGHT (v=N-1 en LEFT conecta con v=N-1 en RIGHT)
                if (v == n - 1) adj.add(CubeCell(CubeFace.RIGHT, u, n - 1))
            }
            CubeFace.RIGHT -> {
                // Arista RIGHT-TOP (v=0 en RIGHT conecta con u=0 en TOP)
                if (v == 0) adj.add(CubeCell(CubeFace.TOP, 0, u))
                
                // Arista vertical RIGHT-LEFT (u=0 en RIGHT conecta con u=0 en LEFT)
                if (u == 0) adj.add(CubeCell(CubeFace.LEFT, 0, v))
                
                // Arista base RIGHT-LEFT (v=N-1 en RIGHT conecta con v=N-1 en LEFT)
                if (v == n - 1) adj.add(CubeCell(CubeFace.LEFT, u, n - 1))
            }
        }
        
        return adj.distinct()
    }
    
    /** Versión simplificada que devuelve solo el primer vecino al cruzar una arista específica */
    fun crossEdge(face: CubeFace, u: Int, v: Int, n: Int): CubeCell? {
        return when (face) {
            CubeFace.TOP -> {
                if (u == n - 1) CubeCell(CubeFace.LEFT, v, 0)
                else if (u == 0) CubeCell(CubeFace.RIGHT, v, 0)
                else null
            }
            CubeFace.LEFT -> {
                if (v == 0) CubeCell(CubeFace.TOP, n - 1, u)
                else if (u == 0) CubeCell(CubeFace.RIGHT, 0, v)
                else if (v == n - 1) CubeCell(CubeFace.RIGHT, u, n - 1)
                else null
            }
            CubeFace.RIGHT -> {
                if (v == 0) CubeCell(CubeFace.TOP, 0, u)
                else if (u == 0) CubeCell(CubeFace.LEFT, 0, v)
                else if (v == n - 1) CubeCell(CubeFace.LEFT, u, n - 1)
                else null
            }
        }
    }

    /** 
     * Comprueba si dos celdas son adyacentes en la topología de cubo.
     * Mapea Cell(row, col, z) -> CubeCell(face, u, v) y consulta getAdjacent.
     */
    fun areAdjacent(a: Cell, b: Cell, n: Int): Boolean {
        val faceA = CubeFace.entries.getOrNull(a.z) ?: return false
        val cubeCellA = CubeCell(faceA, a.row, a.col)
        
        val neighbors = getAdjacent(cubeCellA, n)
        return neighbors.any { 
            it.face.ordinal == b.z && it.u == b.row && it.v == b.col
        }
    }
}
