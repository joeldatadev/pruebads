package com.hoshiraflow.domain.usecase

import com.hoshiraflow.domain.model.*
import com.hoshiraflow.domain.util.CubeEdgeMap
import kotlin.random.Random

/**
 * Generador de "Arquitectura 3D" (Cubes over Cubes).
 * Utiliza la topología CUBE (3 caras superficiales) pero en lugar de un cubo sólido,
 * proyecta una altura H(u,v) para crear formas volumétricas complejas.
 */
class GenerateVolumetricLevelUseCase {

    operator fun invoke(seed: Long, index: Int, n: Int = 5, shapeType: String = "RANDOM"): Board {
        val random = Random(seed + index)
        
        // 1. Definir el mapa de alturas H(u, v) para la rejilla N x N
        val heights = Array(n) { IntArray(n) }
        when (shapeType) {
            "STAIRS" -> {
                for (u in 0 until n) {
                    for (v in 0 until n) {
                        heights[u][v] = n - (u + v) / 2
                    }
                }
            }
            "PYRAMID" -> {
                val mid = n / 2
                for (u in 0 until n) {
                    for (v in 0 until n) {
                        heights[u][v] = (n - kotlin.math.max(kotlin.math.abs(u - mid), kotlin.math.abs(v - mid))).coerceAtLeast(1)
                    }
                }
            }
            "TOWER" -> {
                val mid = n / 2
                for (u in 0 until n) {
                    for (v in 0 until n) {
                        heights[u][v] = if (u == mid && v == mid) n else 1
                    }
                }
            }
            else -> {
                // Generar un terreno aleatorio pero suave
                var base = 1 + random.nextInt(2)
                for (u in 0 until n) {
                    for (v in 0 until n) {
                        heights[u][v] = (base + random.nextInt(3)).coerceIn(1, n)
                        if (random.nextFloat() > 0.7) base = (base + 1).coerceAtMost(n)
                    }
                }
            }
        }

        // 2. Mapear qué celdas de las 3 caras son visibles (superficie del volumen)
        val visibleCells = mutableSetOf<Cell>()

        // Cara TOP (Z=0): Siempre es visible la parte superior de cada columna que tiene altura > 0
        // En nuestra topología CUBE, la cara TOP es un grid plano. La "altura" la simulamos 
        // desactivando celdas o remapeando. Para mantener compatibilidad con CubeEdgeMap,
        // usamos la superficie exterior.
        for (u in 0 until n) {
            for (v in 0 until n) {
                if (heights[u][v] > 0) {
                    visibleCells.add(Cell(u, v, CubeFace.TOP.ordinal))
                }
            }
        }

        // Cara LEFT (Z=1): Visible si la columna a su "derecha" (u-1) es más baja
        for (u in 0 until n) {
            for (v in 0 until n) {
                val h = heights[u][v]
                val hLeft = if (u > 0) heights[u - 1][v] else 0
                // Si esta celda es más alta que su vecina izquierda, sus caras laterales LEFT son visibles
                // Pero en el modelo de 3 caras, simplificamos:
                if (h > hLeft) visibleCells.add(Cell(u, v, CubeFace.LEFT.ordinal))
            }
        }

        // Cara RIGHT (Z=2): Visible si la columna a su "izquierda" (v-1) es más baja
        for (u in 0 until n) {
            for (v in 0 until n) {
                val h = heights[u][v]
                val hRight = if (v > 0) heights[u][v - 1] else 0
                if (h > hRight) visibleCells.add(Cell(u, v, CubeFace.RIGHT.ordinal))
            }
        }

        // 3. Generar caminos usando la topología de adyacencia del cubo
        val path = findHamiltonianPath(visibleCells.toList(), n, random, 2000)
        
        val cellTypes = mutableMapOf<Cell, CellType>()
        // Llenar el mapa completo de celdas posibles marcando como Void las no visibles o no usadas
        for (f in 0..2) {
            for (u in 0 until n) {
                for (v in 0 until n) {
                    val c = Cell(u, v, f)
                    if (c in path) {
                        cellTypes[c] = CellType.Empty
                    } else {
                        cellTypes[c] = CellType.Void
                    }
                }
            }
        }

        val numColors = when {
            path.size < 15 -> 3
            path.size < 30 -> 4
            path.size < 50 -> 6
            else -> 8
        }

        val segments = splitIntoSegments(path, numColors, random)
        val nodes = segments.mapIndexed { i, segment ->
            val color = PuzzleColor.entries[i % PuzzleColor.entries.size]
            val pairId = i + 1
            listOf(Node(segment.first(), color, pairId), Node(segment.last(), color, pairId))
        }.flatten()

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
                val next = unvisited.minByOrNull { neighbor ->
                    getNeighbors(neighbor, n, cellSet).count { it !in visited }
                } ?: unvisited.first()
                path.add(next)
                visited.add(next)
            }
            if (path.size >= targetSize * 0.7) return path
        }
        return allCells.shuffled(random)
    }

    private fun getNeighbors(cell: Cell, n: Int, cellSet: Set<Cell>): List<Cell> {
        val adj = mutableListOf<Cell>()
        val dRow = listOf(1, -1, 0, 0)
        val dCol = listOf(0, 0, 1, -1)
        for (i in 0..3) {
            val nc = Cell(cell.row + dRow[i], cell.col + dCol[i], cell.z)
            if (nc in cellSet) adj.add(nc)
        }
        val face = CubeFace.entries[cell.z]
        CubeEdgeMap.crossEdges(face, cell.row, cell.col, n).forEach {
            val nc = Cell(it.u, it.v, it.face.ordinal)
            if (nc in cellSet) adj.add(nc)
        }
        return adj
    }

    private fun splitIntoSegments(path: List<Cell>, numColors: Int, random: Random): List<List<Cell>> {
        if (path.isEmpty()) return emptyList()
        val total = path.size
        val actualColors = if (total < numColors * 2) (total / 2).coerceAtLeast(1) else numColors
        val segments = mutableListOf<List<Cell>>()
        val sectionSize = total / actualColors
        var currentStart = 0
        for (i in 0 until actualColors) {
            val end = if (i == actualColors - 1) total else (currentStart + sectionSize).coerceAtMost(total)
            if (end - currentStart >= 2) {
                segments.add(path.subList(currentStart, end))
            }
            currentStart = end
        }
        return segments.shuffled(random)
    }
}
