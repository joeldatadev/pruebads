package com.hoshiraflow.domain.usecase

import com.hoshiraflow.domain.model.*
import com.hoshiraflow.domain.util.CubeEdgeMap
import kotlin.math.roundToInt
import kotlin.random.Random

class GenerateCubeLevelUseCase {

    operator fun invoke(seed: Long, index: Int, n: Int = 4, blocks: Int = 1): Board {
        val cells = allCells(n, blocks)
        val segments = generateSegments(seed, index, n, blocks)

        val nodes = mutableListOf<Node>()
        segments.forEachIndexed { i, segment ->
            val color = PuzzleColor.entries[i % PuzzleColor.entries.size]
            val pairId = i + 1
            nodes.add(Node(segment.first(), color, pairId))
            nodes.add(Node(segment.last(), color, pairId))
        }

        return Board(
            rows = n,
            cols = n,
            nodes = nodes,
            cellTypes = cells.associateWith { CellType.Empty },
            topology = BoardTopology.CUBE,
            cubeBlocks = blocks
        )
    }

    /** Partición de TODAS las celdas en caminos contiguos (solución del nivel). */
    internal fun generateSegments(seed: Long, index: Int, n: Int, blocks: Int): List<List<Cell>> {
        val graph = buildGraph(allCells(n, blocks), n, blocks)
        val baseColors = when { n <= 3 -> 3; n <= 5 -> 5; else -> 7 }
        val numColors = (baseColors + (blocks - 1) * 2)
            .coerceAtMost(PuzzleColor.entries.size - blocks)
        val paths = findCover(graph, n, blocks, seed, index)
        return splitIntoSegments(paths, numColors, Random(seed + index))
    }

    private fun allCells(n: Int, blocks: Int): List<Cell> = buildList {
        for (f in 0 until blocks * 3) for (u in 0 until n) for (v in 0 until n) add(Cell(u, v, f))
    }

    private fun buildGraph(cells: List<Cell>, n: Int, blocks: Int): Map<Cell, List<Cell>> {
        val set = cells.toHashSet()
        return cells.associateWith { c ->
            buildList {
                for ((dr, dc) in DIRECTIONS) {
                    val nc = Cell(c.row + dr, c.col + dc, c.z)
                    if (nc in set) add(nc)
                }
                CubeEdgeMap.neighborsOf(c, n, blocks).filterTo(this) { it in set }
            }
        }
    }

    /** Un solo camino hamiltoniano si la búsqueda lo encuentra; si no, uno por bloque. */
    private fun findCover(
        graph: Map<Cell, List<Cell>>, n: Int, blocks: Int, seed: Long, index: Int
    ): List<List<Cell>> {
        val minDegree = graph.values.minOf { it.size }
        val starts = graph.keys.filter { graph.getValue(it).size == minDegree }

        repeat(MAX_ATTEMPTS) { attempt ->
            val rnd = Random(seed * 31 + index * 1_000_003L + attempt)
            val path = HamiltonianSearch(graph, rnd, MAX_STEPS).run(starts.random(rnd))
            if (path != null) return listOf(path)
        }
        // Respaldo garantizado: cada bloque se recorre con un camino construido a mano.
        return (0 until blocks).map { constructiveBlockPath(n, it) }
    }

    /** TOP en serpiente que termina en TOP(n-1,0) y luego el "anillo" LEFT/RIGHT fila a fila. */
    private fun constructiveBlockPath(n: Int, block: Int): List<Cell> {
        val top = CubeEdgeMap.zOf(block, CubeFace.TOP)
        val left = CubeEdgeMap.zOf(block, CubeFace.LEFT)
        val right = CubeEdgeMap.zOf(block, CubeFace.RIGHT)
        val path = ArrayList<Cell>(3 * n * n)

        for (u in 0 until n) {
            val vs = if ((n - 1 - u) % 2 == 0) (n - 1 downTo 0) else (0 until n)
            for (v in vs) path.add(Cell(u, v, top))
        }
        for (v in 0 until n) {
            if (v % 2 == 0) {
                for (u in n - 1 downTo 0) path.add(Cell(u, v, left))
                for (u in 0 until n) path.add(Cell(u, v, right))
            } else {
                for (u in n - 1 downTo 0) path.add(Cell(u, v, right))
                for (u in 0 until n) path.add(Cell(u, v, left))
            }
        }
        return path
    }

    /** Reparte TODAS las celdas; cada segmento mide al menos MIN_SEGMENT. */
    private fun splitIntoSegments(
        paths: List<List<Cell>>, numColors: Int, random: Random
    ): List<List<Cell>> {
        val total = paths.sumOf { it.size }
        val segments = mutableListOf<List<Cell>>()

        for (path in paths) {
            val k = (numColors * path.size / total.toDouble()).roundToInt()
                .coerceIn(1, path.size / MIN_SEGMENT)
            val lengths = IntArray(k) { MIN_SEGMENT }
            repeat(path.size - k * MIN_SEGMENT) { lengths[random.nextInt(k)]++ }

            var idx = 0
            for (len in lengths) {
                segments.add(path.subList(idx, idx + len))
                idx += len
            }
        }
        return segments.shuffled(random)
    }

    private class HamiltonianSearch(
        private val graph: Map<Cell, List<Cell>>,
        private val random: Random,
        private val maxSteps: Int
    ) {
        private val visited = HashSet<Cell>()
        private val path = ArrayList<Cell>()
        private var steps = 0

        fun run(start: Cell): List<Cell>? {
            visited.add(start); path.add(start)
            return if (dfs()) path.toList() else null
        }

        private fun freeDegree(c: Cell) = graph.getValue(c).count { it !in visited }

        /** Una celda libre sin salida, o más de una con una sola salida, hace imposible completar el camino. */
        private fun isDeadEnd(head: Cell): Boolean {
            val headNeighbors = graph.getValue(head)
            var endpoints = 0
            for (c in graph.keys) {
                if (c in visited) continue
                val degree = freeDegree(c) + if (c in headNeighbors) 1 else 0
                if (degree == 0) return true
                if (degree == 1 && ++endpoints > 1) return true
            }
            return false
        }

        private fun dfs(): Boolean {
            if (path.size == graph.size) return true
            if (++steps > maxSteps) return false

            val candidates = graph.getValue(path.last())
                .filter { it !in visited }
                .shuffled(random)
                .sortedBy { freeDegree(it) } // Warnsdorff

            for (next in candidates) {
                visited.add(next); path.add(next)
                if (!isDeadEnd(next) && dfs()) return true
                visited.remove(next); path.removeAt(path.lastIndex)
                if (steps > maxSteps) return false
            }
            return false
        }
    }

    private companion object {
        val DIRECTIONS = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)
        const val MAX_ATTEMPTS = 30
        const val MAX_STEPS = 20_000
        const val MIN_SEGMENT = 3
    }
}
