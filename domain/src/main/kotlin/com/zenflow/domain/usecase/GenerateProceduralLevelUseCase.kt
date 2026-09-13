package com.zenflow.domain.usecase

/**
 * Ruta destino: domain/src/main/kotlin/com/zenflow/domain/usecase/GenerateProceduralLevelUseCase.kt
 *
 * Puerto Kotlin exacto del generador Python (Warnsdorff + corte en segmentos).
 * Mismo algoritmo, misma garantía: el camino Hamiltoniano generado ES la
 * solución, por lo que el tablero siempre es 100% solucionable
 * (compatible con CheckLevelCompleteUseCase.isLevelComplete).
 *
 * Uso para "Modo Infinito": cada índice + seed produce SIEMPRE el mismo
 * tablero (determinístico), útil para un "Reto Diario" compartible.
 *   val board = GenerateProceduralLevelUseCase()(seed = dayOfYear.toLong(), index = dayOfYear)
 * Para infinito puro (no determinístico), usa seed = System.currentTimeMillis().
 */
import com.zenflow.domain.model.Board
import com.zenflow.domain.model.Cell
import com.zenflow.domain.model.Node
import com.zenflow.domain.model.PuzzleColor
import kotlin.random.Random

class GenerateProceduralLevelUseCase {

    private val directions = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)

    operator fun invoke(seed: Long, index: Int, maxAttempts: Int = 3000): Board {
        val (rows, cols, numColors) = difficultyFor(index)
        val random = Random(seed)
        val path = findHamiltonianPath(rows, cols, random, maxAttempts)
        val segments = splitIntoSegments(path, numColors, random)

        val nodes = segments.mapIndexed { i, segment ->
            val color = PuzzleColor.entries[i % PuzzleColor.entries.size]
            listOf(
                Node(Cell(segment.first().first, segment.first().second), color),
                Node(Cell(segment.last().first, segment.last().second), color)
            )
        }.flatten()

        return Board(rows = rows, cols = cols, nodes = nodes)
    }

    /** Misma curva de dificultad que difficulty_curve() en el script Python. */
    private fun difficultyFor(index: Int): Triple<Int, Int, Int> = when {
        index <= 10 -> Triple(5, 5, 3)
        index <= 25 -> Triple(5, 5, 4)
        index <= 40 -> Triple(6, 6, 5)
        index <= 55 -> Triple(6, 6, 6)
        index <= 70 -> Triple(7, 7, 7)
        index <= 85 -> Triple(7, 7, 8)
        index <= 95 -> Triple(8, 8, 9)
        else -> Triple(8, 8, 10) // a partir de aquí, infinito se mantiene en el tope de dificultad
    }

    private fun onwardCount(cell: Pair<Int, Int>, visited: Set<Pair<Int, Int>>, rows: Int, cols: Int): Int {
        var count = 0
        for ((dr, dc) in directions) {
            val n = cell.first + dr to cell.second + dc
            if (n.first in 0 until rows && n.second in 0 until cols && n !in visited) count++
        }
        return count
    }

    private fun findHamiltonianPath(rows: Int, cols: Int, random: Random, maxAttempts: Int): List<Pair<Int, Int>> {
        val totalCells = rows * cols

        repeat(maxAttempts) {
            val start = random.nextInt(rows) to random.nextInt(cols)
            val visited = linkedSetOf(start)
            val path = mutableListOf(start)

            while (path.size < totalCells) {
                val current = path.last()
                val candidates = directions.mapNotNull { (dr, dc) ->
                    val n = current.first + dr to current.second + dc
                    if (n.first in 0 until rows && n.second in 0 until cols && n !in visited) n else null
                }.shuffled(random).sortedBy { onwardCount(it, visited, rows, cols) }

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
