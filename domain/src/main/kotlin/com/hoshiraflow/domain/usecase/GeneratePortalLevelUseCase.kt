package com.hoshiraflow.domain.usecase

import com.hoshiraflow.domain.model.Board
import com.hoshiraflow.domain.model.BoardShape
import com.hoshiraflow.domain.model.Cell
import com.hoshiraflow.domain.model.CellType
import com.hoshiraflow.domain.model.Node
import com.hoshiraflow.domain.model.PuzzleColor
import kotlin.random.Random

/**
 * Generador de niveles que incluyen portales.
 * Los portales se tratan como casillas adyacentes durante la generación del camino
 * para asegurar que el nivel sea resoluble utilizando el teletransporte.
 */
class GeneratePortalLevelUseCase {

    private val directions = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)

    operator fun invoke(seed: Long, index: Int, shape: BoardShape? = null): Board {
        val (defaultRows, defaultCols, numColors) = difficultyFor(index)
        val rows = shape?.height ?: defaultRows
        val cols = shape?.width ?: defaultCols
        
        var attempt = 0
        while (attempt < 100) {
            val random = Random(seed + attempt)
            val cellTypes = shape?.getCellTypes()?.toMutableMap() ?: mutableMapOf()
            
            // 1. Colocar 1 o 2 pares de portales en posiciones no adyacentes (de entre las celdas jugables)
            val numPortalPairs = if (index > 20) 2 else 1
            val allCells = (0 until rows).flatMap { r -> (0 until cols).map { c -> Cell(r, c) } }
                .filter { cellTypes[it] != CellType.Void }
                .shuffled(random)
            
            val portals = mutableListOf<Cell>()
            var pairId = 0
            for (cell in allCells) {
                if (portals.size >= numPortalPairs * 2) break
                
                // Verificar que no sea adyacente a otros portales ya colocados
                val isTooClose = portals.any { it.isAdjacentTo(cell) || it == cell }
                if (!isTooClose) {
                    portals.add(cell)
                    cellTypes[cell] = CellType.Portal(pairId / 2)
                    pairId++
                }
            }

            if (portals.size < numPortalPairs * 2) {
                attempt++
                continue
            }

            // Mapeo de conexiones de portales para el generador
            val portalConnections = mutableMapOf<Cell, Cell>()
            for (i in 0 until numPortalPairs) {
                val p1 = portals[i * 2]
                val p2 = portals[i * 2 + 1]
                portalConnections[p1] = p2
                portalConnections[p2] = p1
            }

            // 2. Intentar encontrar un camino Hamiltoniano que use los portales
            val path = findHamiltonianPath(rows, cols, cellTypes, portalConnections, random, 500)
            
            val voidCount = cellTypes.values.count { it == CellType.Void }
            val targetCellsCount = rows * cols - voidCount

            if (path.size == targetCellsCount) {
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
        
        // Fallback a nivel procedimental normal si no se encuentra solución con portales
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

            // Si empezamos en un portal, saltamos inmediatamente (opcional, pero ayuda a la conectividad)
            portalConnections[start]?.let { 
                if (it !in visited && cellTypes[it] != CellType.Void) {
                    visited.add(it)
                    path.add(it)
                }
            }

            while (path.size < totalCells) {
                val current = path.last()
                
                // Candidatos adyacentes normales
                val normalCandidates = directions.mapNotNull { (dr, dc) ->
                    val n = Cell(current.row + dr, current.col + dc)
                    if (n.row in 0 until rows && n.col in 0 until cols && cellTypes[n] != CellType.Void && n !in visited) n else null
                }

                // Si estamos en un portal, su pareja es un candidato directo (salto)
                val portalJump = portalConnections[current]?.takeIf { it !in visited && cellTypes[it] != CellType.Void }
                
                val allCandidates = if (portalJump != null) {
                    normalCandidates + portalJump
                } else {
                    normalCandidates
                }.shuffled(random).sortedBy { cell ->
                    onwardCount(cell, visited, rows, cols, cellTypes, portalConnections)
                }

                if (allCandidates.isEmpty()) break

                val next = allCandidates.first()
                visited.add(next)
                path.add(next)
                
                // Si el movimiento fue a un portal, debemos añadir automáticamente su salida al camino
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

    private fun onwardCount(
        cell: Cell,
        visited: Set<Cell>,
        rows: Int,
        cols: Int,
        cellTypes: Map<Cell, CellType>,
        portalConnections: Map<Cell, Cell>
    ): Int {
        var count = 0
        // Vecinos normales
        for ((dr, dc) in directions) {
            val n = Cell(cell.row + dr, cell.col + dc)
            if (n.row in 0 until rows && n.col in 0 until cols && cellTypes[n] != CellType.Void && n !in visited) count++
        }
        // Conexión por portal
        portalConnections[cell]?.let { if (it !in visited && cellTypes[it] != CellType.Void) count++ }
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
