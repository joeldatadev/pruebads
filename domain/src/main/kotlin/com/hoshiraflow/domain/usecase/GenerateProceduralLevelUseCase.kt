package com.hoshiraflow.domain.usecase

import com.hoshiraflow.domain.model.Board
import com.hoshiraflow.domain.model.BoardShape
import com.hoshiraflow.domain.model.Cell
import com.hoshiraflow.domain.model.CellType
import com.hoshiraflow.domain.model.Node
import com.hoshiraflow.domain.model.PuzzleColor
import kotlin.random.Random

class GenerateProceduralLevelUseCase {

    private val directions = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)

    operator fun invoke(seed: Long, index: Int, maxAttempts: Int = 3000, shape: BoardShape? = null): Board {
        val (defaultRows, defaultCols, defaultColors) = difficultyFor(index)
        val rows = shape?.height ?: defaultRows
        val cols = shape?.width ?: defaultCols
        val cellTypes = shape?.getCellTypes().orEmpty()

        val voidCount = cellTypes.values.count { it == CellType.Void }
        val playableCellsCount = rows * cols - voidCount

        val random = Random(seed)
        val numColors = if (shape != null) {
            when {
                playableCellsCount <= 25 -> random.nextInt(4, 6)
                playableCellsCount <= 49 -> random.nextInt(5, 7)
                playableCellsCount <= 81 -> random.nextInt(7, 9)
                playableCellsCount <= 121 -> random.nextInt(9, 11)
                else -> random.nextInt(11, 15)
            }
        } else {
            defaultColors
        }

        val path = findHamiltonianPath(rows, cols, cellTypes, random, maxAttempts)
        val segments = splitIntoSegments(path, numColors, random)

        val nodes = segments.mapIndexed { i, segment ->
            val color = PuzzleColor.entries[i % PuzzleColor.entries.size]
            val pairId = i + 1
            listOf(
                Node(Cell(segment.first().first, segment.first().second), color, pairId),
                Node(Cell(segment.last().first, segment.last().second), color, pairId)
            )
        }.flatten()

        return Board(rows = rows, cols = cols, nodes = nodes, cellTypes = cellTypes)
    }

    private fun difficultyFor(index: Int): Triple<Int, Int, Int> = when {
        index <= 10 -> Triple(5, 5, 3)
        index <= 25 -> Triple(5, 5, 4)
        index <= 40 -> Triple(6, 6, 5)
        index <= 55 -> Triple(6, 6, 6)
        index <= 70 -> Triple(7, 7, 7)
        index <= 85 -> Triple(7, 7, 8)
        index <= 95 -> Triple(8, 8, 9)
        else -> Triple(8, 8, 10)
    }

    private fun onwardCount(
        cell: Pair<Int, Int>, 
        visited: Set<Pair<Int, Int>>, 
        rows: Int, 
        cols: Int, 
        cellTypes: Map<Cell, CellType>
    ): Int {
        var count = 0
        for ((dr, dc) in directions) {
            val nr = cell.first + dr
            val nc = cell.second + dc
            if (nr in 0 until rows && nc in 0 until cols && (nr to nc) !in visited) {
                if (cellTypes[Cell(nr, nc)] != CellType.Void) {
                    count++
                }
            }
        }
        return count
    }

    private fun findHamiltonianPath(
        rows: Int, 
        cols: Int, 
        cellTypes: Map<Cell, CellType>, 
        random: Random, 
        maxAttempts: Int
    ): List<Pair<Int, Int>> {
        val voidCount = cellTypes.values.count { it == CellType.Void }
        val totalCells = rows * cols - voidCount

        val playableCells = (0 until rows).flatMap { r ->
            (0 until cols).map { c -> r to c }
        }.filter { cellTypes[Cell(it.first, it.second)] != CellType.Void }

        if (playableCells.isEmpty()) return emptyList()

        repeat(maxAttempts) {
            val start = playableCells.random(random)
            val visited = linkedSetOf(start)
            val path = mutableListOf(start)

            while (path.size < totalCells) {
                val current = path.last()
                val candidates = directions.mapNotNull { (dr, dc) ->
                    val nr = current.first + dr
                    val nc = current.second + dc
                    if (nr in 0 until rows && nc in 0 until cols && (nr to nc) !in visited && cellTypes[Cell(nr, nc)] != CellType.Void) {
                        nr to nc
                    } else null
                }.shuffled(random).sortedBy { onwardCount(it, visited, rows, cols, cellTypes) }

                if (candidates.isEmpty()) break

                val next = candidates.first()
                visited.add(next)
                path.add(next)
            }

            if (path.size == totalCells) return path
        }

        throw IllegalStateException("No se encontró camino Hamiltoniano para ${rows}x$cols en $maxAttempts intentos")
    }

    private fun splitIntoSegments(
        path: List<Pair<Int, Int>>,
        numColors: Int,
        random: Random
    ): List<List<Pair<Int, Int>>> {
        val total = path.size
        val base = total / numColors
        val lengths = IntArray(numColors) { base }
        val remainder = total - base * numColors
        for (i in 0 until remainder) lengths[i % numColors] += 1

        for (i in lengths.indices) {
            while (lengths[i] < 2) {
                val donor = lengths.indices.maxBy { lengths[it] }
                lengths[donor] -= 1
                lengths[i] += 1
            }
        }

        val segments = mutableListOf<List<Pair<Int, Int>>>()
        var idx = 0
        for (length in lengths) {
            segments.add(path.subList(idx, idx + length))
            idx += length
        }
        return segments.shuffled(random)
    }
}
