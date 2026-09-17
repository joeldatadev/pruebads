package com.hoshiraflow.domain.usecase

import com.hoshiraflow.domain.model.Board
import com.hoshiraflow.domain.model.BoardShape
import com.hoshiraflow.domain.model.Cell
import com.hoshiraflow.domain.model.CellType
import com.hoshiraflow.domain.model.Node
import com.hoshiraflow.domain.model.PuzzleColor
import kotlin.random.Random

/**
 * Generador de niveles que incluyen interruptores de color (Switch).
 * Los interruptores permiten que un camino cambie de color a mitad de recorrido.
 * La generación asegura que el camino resultante conecte los nodos correctos
 * a pesar de la mutación de color.
 */
class GenerateSwitchLevelUseCase {

    private val directions = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)

    operator fun invoke(seed: Long, index: Int, shape: BoardShape? = null): Board {
        val (defaultRows, defaultCols, numColors) = difficultyFor(index)
        val rows = shape?.height ?: defaultRows
        val cols = shape?.width ?: defaultCols
        
        var attempt = 0
        while (attempt < 100) {
            val random = Random(seed + attempt)
            val cellTypes = shape?.getCellTypes()?.toMutableMap() ?: mutableMapOf()
            
            // 1. Generar primero un camino Hamiltoniano completo para asegurar solvencia
            val fullPath = findHamiltonianPath(rows, cols, cellTypes, random, 1000)
            if (fullPath.isEmpty()) {
                attempt++
                continue
            }

            // 2. Dividir en segmentos para los colores
            val segments = splitIntoSegments(fullPath, numColors, random)
            
            val finalNodes = mutableListOf<Node>()
            
            // 3. Colocar 1 o 2 interruptores en segmentos aleatorios (que no sean el primero o el último nodo)
            val numSwitches = if (index > 25) 2 else 1
            val segmentsToSwitch = segments.filter { it.size > 3 }.shuffled(random).take(numSwitches)
            
            segments.forEachIndexed { i, segment ->
                val baseColor = PuzzleColor.entries[i % PuzzleColor.entries.size]
                
                if (segment in segmentsToSwitch) {
                    // Elegimos una celda interna para el interruptor
                    val switchIdx = random.nextInt(1, segment.size - 1)
                    val switchCell = segment[switchIdx]
                    
                    // El targetColor será el color original para que el camino "vuelva" a ser el que era
                    val targetColor = PuzzleColor.entries[(i + 1) % PuzzleColor.entries.size]
                    cellTypes[switchCell] = CellType.Switch(targetColor)
                    
                    // Los nodos del segmento siguen siendo del baseColor
                    finalNodes.add(Node(segment.first(), baseColor))
                    finalNodes.add(Node(segment.last(), baseColor))
                } else {
                    finalNodes.add(Node(segment.first(), baseColor))
                    finalNodes.add(Node(segment.last(), baseColor))
                }
            }

            return Board(
                rows = rows,
                cols = cols,
                nodes = finalNodes,
                cellTypes = cellTypes
            )
        }
        
        // Fallback a nivel procedimental normal
        return GenerateProceduralLevelUseCase().invoke(seed, index, shape = shape)
    }

    private fun difficultyFor(index: Int): Triple<Int, Int, Int> = when {
        index <= 15 -> Triple(5, 5, 3)
        index <= 30 -> Triple(6, 6, 4)
        else -> Triple(7, 7, 5)
    }

    private fun findHamiltonianPath(
        rows: Int, 
        cols: Int, 
        cellTypes: Map<Cell, CellType>, 
        random: Random, 
        maxAttempts: Int
    ): List<Cell> {
        val voidCount = cellTypes.values.count { it == CellType.Void }
        val totalCells = rows * cols - voidCount

        val playableCells = (0 until rows).flatMap { r ->
            (0 until cols).map { c -> Cell(r, c) }
        }.filter { cellTypes[it] != CellType.Void }

        if (playableCells.isEmpty()) return emptyList()

        repeat(maxAttempts) {
            val start = playableCells.random(random)
            val visited = linkedSetOf(start)
            val path = mutableListOf(start)
            while (path.size < totalCells) {
                val current = path.last()
                val candidates = directions.mapNotNull { (dr, dc) ->
                    val n = Cell(current.row + dr, current.col + dc)
                    if (n.row in 0 until rows && n.col in 0 until cols && cellTypes[n] != CellType.Void && n !in visited) n else null
                }.shuffled(random).sortedBy { cell ->
                    directions.count { (dr, dc) ->
                        val n = Cell(cell.row + dr, cell.col + dc)
                        n.row in 0 until rows && n.col in 0 until cols && cellTypes[n] != CellType.Void && n !in visited
                    }
                }
                if (candidates.isEmpty()) break
                val next = candidates.first()
                visited.add(next)
                path.add(next)
            }
            if (path.size == totalCells) return path
        }
        return emptyList()
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
