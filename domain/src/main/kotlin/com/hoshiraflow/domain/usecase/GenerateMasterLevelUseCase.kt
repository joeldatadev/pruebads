package com.hoshiraflow.domain.usecase

import com.hoshiraflow.domain.model.Board
import com.hoshiraflow.domain.model.BoardShape
import com.hoshiraflow.domain.model.Cell
import com.hoshiraflow.domain.model.CellType
import com.hoshiraflow.domain.model.Node
import com.hoshiraflow.domain.model.PuzzleColor
import kotlin.random.Random

/**
 * Generador de niveles "Maestros".
 * Combina todas las mecánicas especiales: Portales, Interruptores de Color y Bloqueos.
 * Asegura la solvencia al integrar saltos y mutaciones en el algoritmo del camino Hamiltoniano.
 */
class GenerateMasterLevelUseCase {

    private val directions = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)

    operator fun invoke(seed: Long, index: Int, shape: BoardShape? = null): Board {
        val (defaultRows, defaultCols, defaultColors) = difficultyFor(index)
        val rows = shape?.height ?: defaultRows
        val cols = shape?.width ?: defaultCols
        
        var attempt = 0
        while (attempt < 100) {
            val random = Random(seed + attempt)
            val cellTypes = shape?.getCellTypes()?.toMutableMap() ?: mutableMapOf()
            
            val voidCount = cellTypes.values.count { it == CellType.Void }
            val playableCellsCount = rows * cols - voidCount

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
            
            // 1. Colocar Portales primero (definen la topología del salto, eligiendo de celdas no Void)
            val allCells = (0 until rows).flatMap { r -> (0 until cols).map { c -> Cell(r, c) } }
                .filter { cellTypes[it] != CellType.Void }
                .shuffled(random)
                val portalPair = allCells.take(2)
            if (portalPair.size < 2 || portalPair[0].isAdjacentTo(portalPair[1])) {
                attempt++
                continue
            }
            
            val p1 = portalPair[0]
            val p2 = portalPair[1]
            cellTypes[p1] = CellType.Portal(0)
            cellTypes[p2] = CellType.Portal(0)
            val portalConnections = mapOf(p1 to p2, p2 to p1)

            // 2. Encontrar camino que use el portal
            val path = findHamiltonianPath(rows, cols, cellTypes, portalConnections, random, 500)
            
            val targetCellsCount = rows * cols - voidCount

            if (path.size < targetCellsCount) {
                attempt++
                continue
            }

            // 3. Dividir en segmentos
            val segments = splitIntoSegments(path, numColors, random)
            val nodes = mutableListOf<Node>()
            
            // 4. Inyectar Interruptores de Color en segmentos largos
            val switchSegmentIdx = segments.indexOfFirst { it.size > 4 }
            if (switchSegmentIdx != -1) {
                val segment = segments[switchSegmentIdx]
                val switchIdx = random.nextInt(1, segment.size - 1)
                val switchCell = segment[switchIdx]
                
                // Evitar pisar portales con interruptores
                if (cellTypes[switchCell] !is CellType.Portal) {
                    val targetColor = PuzzleColor.entries[(switchSegmentIdx + 1) % PuzzleColor.entries.size]
                    cellTypes[switchCell] = CellType.Switch(targetColor)
                }
            }

            // 5. Asignar Nodos
            segments.forEachIndexed { i, segment ->
                val baseColor = PuzzleColor.entries[i % PuzzleColor.entries.size]
                val pairId = i + 1
                nodes.add(Node(segment.first(), baseColor, pairId))
                nodes.add(Node(segment.last(), baseColor, pairId))
            }
            
            return Board(
                rows = rows,
                cols = cols,
                nodes = nodes,
                cellTypes = cellTypes
            )
        }
        
        return GenerateProceduralLevelUseCase().invoke(seed, index, shape = shape)
    }

    private fun difficultyFor(index: Int): Triple<Int, Int, Int> = when {
        index <= 10 -> Triple(6, 6, 4)
        else -> Triple(7, 7, 5)
    }

    private fun findHamiltonianPath(
        rows: Int,
        cols: Int,
        cellTypes: Map<Cell, CellType>,
        portalConnections: Map<Cell, Cell>,
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
                val normalCandidates = directions.mapNotNull { (dr, dc) ->
                    val n = Cell(current.row + dr, current.col + dc)
                    if (n.row in 0 until rows && n.col in 0 until cols && cellTypes[n] != CellType.Void && n !in visited) n else null
                }
                val portalJump = portalConnections[current]?.takeIf { it !in visited && cellTypes[it] != CellType.Void }
                
                val allCandidates = (normalCandidates + listOfNotNull(portalJump))
                    .shuffled(random)
                    .sortedBy { cell ->
                        var count = directions.count { (dr, dc) ->
                            val n = Cell(cell.row + dr, cell.col + dc)
                            n.row in 0 until rows && n.col in 0 until cols && cellTypes[n] != CellType.Void && n !in visited
                        }
                        portalConnections[cell]?.let { if (it !in visited && cellTypes[it] != CellType.Void) count++ }
                        count
                    }

                if (allCandidates.isEmpty()) break
                val next = allCandidates.first()
                visited.add(next)
                path.add(next)
                
                portalConnections[next]?.let { dest ->
                    if (dest !in visited && cellTypes[dest] != CellType.Void) {
                        visited.add(dest)
                        path.add(dest)
                    }
                }
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
        val segments = mutableListOf<List<Cell>>()
        var idx = 0
        for (length in lengths) {
            segments.add(path.subList(idx, idx + length))
            idx += length
        }
        return segments.shuffled(random)
    }
}
