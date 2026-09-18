package com.hoshiraflow.domain.usecase

import com.hoshiraflow.domain.model.*
import com.hoshiraflow.domain.util.CubeEdgeMap

/**
 * Genera un tablero de cubo de 3 caras para diagnóstico.
 */
class GenerateEmptyCubeUseCase {

    operator fun invoke(n: Int): Board {
        val cellTypes = mutableMapOf<Cell, CellType>()
        val nodes = mutableListOf<Node>()

        for (faceIdx in 0..2) {
            for (u in 0 until n) {
                for (v in 0 until n) {
                    cellTypes[Cell(u, v, faceIdx)] = CellType.Empty
                }
            }
        }

        fun crossOrThrow(face: CubeFace, u: Int, v: Int): CubeEdgeMap.Neighbor =
            CubeEdgeMap.crossEdges(face, u, v, n).firstOrNull()
                ?: error("Celda $face($u,$v) no tiene arista compartida con otra cara para n=$n")

        // Par A: Morado -> TOP-LEFT
        val cellA1 = Cell(1, 0, CubeFace.TOP.ordinal)
        val neighborA = crossOrThrow(CubeFace.TOP, 1, 0)
        val cellA2 = Cell(neighborA.u, neighborA.v, neighborA.face.ordinal)
        nodes.add(Node(cellA1, PuzzleColor.PURPLE, 1))
        nodes.add(Node(cellA2, PuzzleColor.PURPLE, 1))

        // Par B: Cian -> TOP-RIGHT
        val cellB1 = Cell(0, 1, CubeFace.TOP.ordinal)
        val neighborB = crossOrThrow(CubeFace.TOP, 0, 1)
        val cellB2 = Cell(neighborB.u, neighborB.v, neighborB.face.ordinal)
        nodes.add(Node(cellB1, PuzzleColor.CYAN, 2))
        nodes.add(Node(cellB2, PuzzleColor.CYAN, 2))

        // Par C: Amarillo -> LEFT-RIGHT
        val cellC1 = Cell(0, 1, CubeFace.LEFT.ordinal)
        val neighborC = crossOrThrow(CubeFace.LEFT, 0, 1)
        val cellC2 = Cell(neighborC.u, neighborC.v, neighborC.face.ordinal)
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
