package com.hoshiraflow.domain.usecase

import com.hoshiraflow.domain.model.*
import com.hoshiraflow.domain.util.CubeEdgeMap
import kotlin.random.Random

/**
 * Generador procedural de niveles para el cubo de 3 caras.
 * Utiliza CubeEdgeMap para garantizar que los caminos fluyan entre caras correctamente.
 */
class GenerateCubeLevelUseCase {

    operator fun invoke(seed: Long, index: Int, n: Int = 4): Board {
        val random = Random(seed + index)
        val allCells = mutableListOf<Cell>()
        for (f in 0..2) {
            for (u in 0 until n) {
                for (v in 0 until n) {
                    allCells.add(Cell(u, v, f))
                }
            }
        }

        val cellTypes = allCells.associateWith { CellType.Empty }.toMutableMap()
        
        // Buscamos un camino que cubra el máximo de celdas posible (Hamiltoniano idealmente)
        val fullPath = findHamiltonianPath(allCells, n, random, 2000)
        
        // Decidimos cuántos colores tendrá el nivel según el tamaño y el índice
        val numColors = when {
            n <= 3 -> 3
            n <= 5 -> 5
            else -> 7
        }

        val segments = splitIntoSegments(fullPath, numColors, random)
        val nodes = mutableListOf<Node>()
        
        segments.forEachIndexed { i, segment ->
            if (segment.size >= 2) {
                val color = PuzzleColor.entries[i % PuzzleColor.entries.size]
                val pairId = i + 1
                nodes.add(Node(segment.first(), color, pairId))
                nodes.add(Node(segment.last(), color, pairId))
            }
        }

        return Board(
            rows = n,
            cols = n,
            nodes = nodes,
            cellTypes = cellTypes,
            topology = BoardTopology.CUBE
        )
    }

    private fun findHamiltonianPath(allCells: List<Cell>, n: Int, random: Random, maxAttempts: Int): List<Cell> {
        val targetSize = allCells.size
        val cellSet = allCells.toSet()

        repeat(maxAttempts) {
            val start = allCells.random(random)
            val path = mutableListOf(start)
            val visited = mutableSetOf(start)

            while (path.size < targetSize) {
                val current = path.last()
                val neighbors = getNeighbors(current, n, cellSet)
                val unvisited = neighbors.filter { it !in visited }.shuffled(random)

                if (unvisited.isEmpty()) break
                
                // Heurística de Warnsdorff para 3D: elegir el vecino con menos vecinos libres
                val next = unvisited.minByOrNull { neighbor ->
                    getNeighbors(neighbor, n, cellSet).count { it !in visited }
                } ?: unvisited.first()

                path.add(next)
                visited.add(next)
            }

            if (path.size >= targetSize * 0.8) return path // Aceptamos 80%+ de cobertura
        }
        return allCells.shuffled(random)
    }

    private fun getNeighbors(cell: Cell, n: Int, cellSet: Set<Cell>): List<Cell> {
        val adj = mutableListOf<Cell>()
        // Vecinos en la misma cara
        val dRow = listOf(1, -1, 0, 0)
        val dCol = listOf(0, 0, 1, -1)
        for (i in 0..3) {
            val nc = Cell(cell.row + dRow[i], cell.col + dCol[i], cell.z)
            if (nc in cellSet) adj.add(nc)
        }
        // Vecinos cruzando aristas
        val face = CubeFace.entries[cell.z]
        CubeEdgeMap.crossEdges(face, cell.row, cell.col, n).forEach {
            val nc = Cell(it.u, it.v, it.face.ordinal)
            if (nc in cellSet) adj.add(nc)
        }
        return adj
    }

    private fun splitIntoSegments(path: List<Cell>, numColors: Int, random: Random): List<List<Cell>> {
        if (path.isEmpty()) return emptyList()
        val segments = mutableListOf<List<Cell>>()
        val total = path.size
        val avgLen = total / numColors
        
        var currentIdx = 0
        repeat(numColors) {
            val len = random.nextInt((avgLen * 0.7).toInt().coerceAtLeast(2), (avgLen * 1.3).toInt().coerceAtLeast(3))
            val endIdx = (currentIdx + len).coerceAtMost(total)
            if (endIdx - currentIdx >= 2) {
                segments.add(path.subList(currentIdx, endIdx))
            }
            currentIdx = endIdx
            if (currentIdx >= total) return@repeat
        }
        return segments
    }
}
