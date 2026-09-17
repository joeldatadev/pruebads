package com.hoshiraflow.domain.usecase

import com.hoshiraflow.domain.model.Board
import com.hoshiraflow.domain.model.BoardShape
import com.hoshiraflow.domain.model.Cell
import com.hoshiraflow.domain.model.CellType
import com.hoshiraflow.domain.model.Node
import com.hoshiraflow.domain.model.PuzzleColor
import kotlin.random.Random

/**
 * Generador de niveles para el modo Desafío.
 * Implementa la estrategia "Wall-First": primero coloca muros y luego genera caminos
 * sobre las celdas restantes para asegurar que el puzzle sea resoluble.
 */
class GenerateChallengeLevelUseCase {

    private val directions = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)

    operator fun invoke(seed: Long, index: Int, blockedPercentage: Float = 0.12f, shape: BoardShape? = null): Board {
        val (defaultRows, defaultCols, numColors) = difficultyFor(index)
        val rows = shape?.height ?: defaultRows
        val cols = shape?.width ?: defaultCols

        // Usamos una combinación de semilla e intentos para mantener el determinismo
        var attempt = 0
        while (attempt < 50) {
            val random = Random(seed + attempt)
            val cellTypes = shape?.getCellTypes()?.toMutableMap() ?: mutableMapOf()
            
            // 1. Selección de celdas para bloquear (solo de entre las celdas que no son Void)
            val allCells = (0 until rows).flatMap { r -> (0 until cols).map { c -> Cell(r, c) } }
                .filter { cellTypes[it] != CellType.Void }
                .shuffled(random)
            
            val totalPlayableCellsCount = allCells.size
            val numToBlock = (totalPlayableCellsCount * blockedPercentage).toInt()
            
            val blockedList = allCells.take(numToBlock)
            blockedList.forEach { cellTypes[it] = CellType.Blocked }
            
            val voidCount = cellTypes.values.count { it == CellType.Void }
            val targetCount = rows * cols - voidCount - numToBlock

            // 2. Intentar encontrar un camino Hamiltoniano en las celdas restantes
            val path = findHamiltonianPath(rows, cols, cellTypes, random, 500)
            
            if (path.size == targetCount) {
                // 3. Dividir en segmentos para los colores
                val segments = splitIntoSegments(path, numColors, random)
                val nodes = segments.mapIndexed { i, segment ->
                    val color = PuzzleColor.entries[i % PuzzleColor.entries.size]
                    listOf(
                        Node(segment.first(), color),
                        Node(segment.last(), color)
                    )
                }.flatten()

                return Board(
                    rows = rows,
                    cols = cols,
                    nodes = nodes,
                    cellTypes = cellTypes
                )
            }
            attempt++
        }
        
        // Fallback final: si fallamos demasiadas veces, generamos un nivel normal sin muros
        return GenerateProceduralLevelUseCase().invoke(seed, index, shape = shape)
    }

    private fun difficultyFor(index: Int): Triple<Int, Int, Int> = when {
        index <= 10 -> Triple(5, 5, 3)
        index <= 25 -> Triple(6, 6, 4)
        else -> Triple(7, 7, 5)
    }

    private fun findHamiltonianPath(
        rows: Int,
        cols: Int,
        cellTypes: Map<Cell, CellType>,
        random: Random,
        maxAttempts: Int
    ): List<Cell> {
        val totalCellsCount = rows * cols
        val blockedCount = cellTypes.values.count { it == CellType.Blocked }
        val voidCount = cellTypes.values.count { it == CellType.Void }
        val targetSize = totalCellsCount - blockedCount - voidCount

        val emptyCells = (0 until rows).flatMap { r -> 
            (0 until cols).map { c -> Cell(r, c) } 
        }.filter { cellTypes[it] != CellType.Blocked && cellTypes[it] != CellType.Void }

        if (emptyCells.isEmpty()) return emptyList()

        repeat(maxAttempts) {
            val start = emptyCells.random(random)
            val visited = linkedSetOf(start)
            val path = mutableListOf(start)

            while (path.size < targetSize) {
                val current = path.last()
                val candidates = directions.mapNotNull { (dr, dc) ->
                    val n = Cell(current.row + dr, current.col + dc)
                    if (n.row in 0 until rows && n.col in 0 until cols && 
                        cellTypes[n] != CellType.Blocked && cellTypes[n] != CellType.Void && n !in visited) n else null
                }.shuffled(random).sortedBy { cell ->
                    onwardCount(cell, visited, rows, cols, cellTypes)
                }

                if (candidates.isEmpty()) break

                val next = candidates.first()
                visited.add(next)
                path.add(next)
            }

            if (path.size == targetSize) return path
        }

        return emptyList()
    }

    private fun onwardCount(
        cell: Cell, 
        visited: Set<Cell>, 
        rows: Int, 
        cols: Int, 
        cellTypes: Map<Cell, CellType>
    ): Int {
        var count = 0
        for ((dr, dc) in directions) {
            val n = Cell(cell.row + dr, cell.col + dc)
            if (n.row in 0 until rows && n.col in 0 until cols && 
                cellTypes[n] != CellType.Blocked && cellTypes[n] != CellType.Void && n !in visited) count++
        }
        return count
    }

    private fun splitIntoSegments(path: List<Cell>, numColors: Int, random: Random): List<List<Cell>> {
        val total = path.size
        val base = total / numColors
        val lengths = IntArray(numColors) { base }
        val remainder = total - base * numColors
        for (i in 0 until remainder) lengths[i % numColors] += 1

        for (i in lengths.indices) {
            while (lengths[i] < 2) {
                val donor = lengths.indices.maxBy { lengths[it] }
                if (lengths[donor] <= 2) break
                lengths[donor] -= 1
                lengths[i] += 1
            }
        }

        val segments = mutableListOf<List<Cell>>()
        var idx = 0
        for (length in lengths) {
            segments.add(path.subList(idx, idx + length))
            idx += length
        }
        return segments.shuffled(random)
    }
}
