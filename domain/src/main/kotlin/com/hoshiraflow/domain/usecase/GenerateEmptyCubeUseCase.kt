package com.hoshiraflow.domain.usecase

import com.hoshiraflow.domain.model.*
import com.hoshiraflow.domain.util.CubeEdgeMap

/**
 * Genera un tablero de cubo vacío (3 caras) con celdas de prueba en las aristas.
 */
class GenerateEmptyCubeUseCase {

    operator fun invoke(n: Int): Board {
        val cellTypes = mutableMapOf<Cell, CellType>()
        val nodes = mutableListOf<Node>()
        
        // Generamos celdas para las 3 caras (z=0:TOP, z=1:LEFT, z=2:RIGHT)
        for (faceIdx in 0..2) {
            for (u in 0 until n) {
                for (v in 0 until n) {
                    val cell = Cell(u, v, faceIdx)
                    cellTypes[cell] = CellType.Empty
                }
            }
        }

        // --- PINTADO DE CELDAS DE PRUEBA EN ARISTAS ---
        
        // Par A: Magenta (PURPLE) -> TOP-LEFT
        // Celda en TOP (u=n-1, v=1) y su vecina en LEFT
        val cellA1 = Cell(n - 1, 1, 0)
        val faceA2 = CubeEdgeMap.crossEdge(CubeFace.TOP, n - 1, 1, n)!!
        val cellA2 = Cell(faceA2.u, faceA2.v, faceA2.face.ordinal)
        nodes.add(Node(cellA1, PuzzleColor.PURPLE, 1))
        nodes.add(Node(cellA2, PuzzleColor.PURPLE, 1))

        // Par B: Cian (CYAN) -> TOP-RIGHT
        // Celda en TOP (u=0, v=1) y su vecina en RIGHT
        val cellB1 = Cell(0, 1, 0)
        val faceB2 = CubeEdgeMap.crossEdge(CubeFace.TOP, 0, 1, n)!!
        val cellB2 = Cell(faceB2.u, faceB2.v, faceB2.face.ordinal)
        nodes.add(Node(cellB1, PuzzleColor.CYAN, 2))
        nodes.add(Node(cellB2, PuzzleColor.CYAN, 2))

        // Par C: Amarillo (YELLOW) -> LEFT-RIGHT (BASE)
        // Celda en LEFT (u=1, v=n-1) y su vecina en RIGHT
        val cellC1 = Cell(1, n - 1, 1)
        val faceC2 = CubeEdgeMap.crossEdge(CubeFace.LEFT, 1, n - 1, n)!!
        val cellC2 = Cell(faceC2.u, faceC2.v, faceC2.face.ordinal)
        nodes.add(Node(cellC1, PuzzleColor.YELLOW, 3))
        nodes.add(Node(cellC2, PuzzleColor.YELLOW, 3))

        return Board(
            rows = n,
            cols = n,
            nodes = nodes,
            cellTypes = cellTypes,
            topology = BoardTopology.CUBE
        )
    }
}
