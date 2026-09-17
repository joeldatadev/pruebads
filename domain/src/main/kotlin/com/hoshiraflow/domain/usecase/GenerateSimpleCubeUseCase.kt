package com.hoshiraflow.domain.usecase

import com.hoshiraflow.domain.model.*
import kotlin.random.Random

/**
 * Generador simplificado de un cubo sólido de 2x2x2.
 * Usado para validar la mecánica isométrica básica (proyección, touch, adyacencia).
 */
class GenerateSimpleCubeUseCase {

    private val directions = listOf(
        Cell(0, 1, 0), Cell(0, -1, 0),
        Cell(1, 0, 0), Cell(-1, 0, 0),
        Cell(0, 0, 1), Cell(0, 0, -1)
    )

    operator fun invoke(seed: Long, index: Int): Board {
        val random = Random(seed)
        val rows = 2
        val cols = 2
        val layers = 2
        
        val activeCells = mutableListOf<Cell>()
        for (z in 0 until layers) {
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    activeCells.add(Cell(r, c, z))
                }
            }
        }

        val cellTypes = activeCells.associateWith { CellType.Empty }.toMutableMap()

        // Generar un camino Hamiltoniano que cubra las 8 celdas
        val path = findHamiltonianPath(activeCells, random, 500)
        
        // Colocar nodos en los extremos del camino
        val color = PuzzleColor.RED
        val nodes = listOf(
            Node(path.first(), color, 1),
            Node(path.last(), color, 1)
        )

        return Board(
            rows = rows,
            cols = cols,
            nodes = nodes,
            cellTypes = cellTypes,
            topology = BoardTopology.ISOMETRIC
        )
    }

    private fun findHamiltonianPath(activeCells: List<Cell>, random: Random, maxAttempts: Int): List<Cell> {
        val boundarySet = activeCells.toSet()
        val targetSize = activeCells.size

        repeat(maxAttempts) {
            val start = activeCells.random(random)
            val path = mutableListOf(start)
            val visited = mutableSetOf(start)

            while (path.size < targetSize) {
                val current = path.last()
                val candidates = directions.map { d ->
                    Cell(current.row + d.row, current.col + d.col, current.z + d.z)
                }.filter { it in boundarySet && it !in visited }
                .shuffled(random)

                if (candidates.isEmpty()) break
                val next = candidates.first()
                path.add(next)
                visited.add(next)
            }
            
            if (path.size == targetSize) return path
        }
        
        return activeCells // Fallback
    }
}
