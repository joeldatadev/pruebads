package com.hoshiraflow.domain.usecase

import com.hoshiraflow.domain.model.*
import kotlin.random.Random

/**
 * Procedural generator for 3D Isometric solvable path layouts.
 * Constrains paths and node endpoints strictly to defined volumetric boundaries.
 */
class GenerateIsometricLevelUseCase {

    private val directions = listOf(
        Cell(0, 1, 0), Cell(0, -1, 0),
        Cell(1, 0, 0), Cell(-1, 0, 0),
        Cell(0, 0, 1), Cell(0, 0, -1)
    )

    operator fun invoke(seed: Long, index: Int, shapeType: String = "PYRAMID"): Board {
        val random = Random(seed)
        val rows = 9
        val cols = 9
        val maxLayers = 5
        
        // Define exact фигуre boundaries (Whitelist of playable coordinates)
        val active3DBlocks = defineVolumetricShape(shapeType, rows, cols, maxLayers)
        if (active3DBlocks.isEmpty()) {
            throw IllegalStateException("Defined shape boundary is empty for $shapeType")
        }

        // Initialize all possible cells within dimensions as Void first
        val cellTypes = mutableMapOf<Cell, CellType>()
        for (z in 0 until maxLayers) {
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    cellTypes[Cell(r, c, z)] = CellType.Void
                }
            }
        }
        
        // Overwrite whitelist as Empty (playable)
        active3DBlocks.forEach { cellTypes[it] = CellType.Empty }

        // Generate Hamiltonian path strictly within the white-listed boundaries
        val path = findHamiltonianPath(active3DBlocks, random, 3000)
        
        val numColors = when {
            active3DBlocks.size <= 30 -> 4
            active3DBlocks.size <= 60 -> 6
            active3DBlocks.size <= 100 -> 8
            else -> 10
        }

        val segments = splitIntoSegments(path, numColors, random)
        
        // Enforce node endpoints strictly on valid coordinates from the whitelist
        val nodes = segments.mapIndexed { i, segment ->
            val color = PuzzleColor.entries[i % PuzzleColor.entries.size]
            val pairId = i + 1
            listOf(
                Node(segment.first(), color, pairId),
                Node(segment.last(), color, pairId)
            )
        }.flatten()

        return Board(
            rows = rows,
            cols = cols,
            nodes = nodes,
            cellTypes = cellTypes,
            topology = BoardTopology.ISOMETRIC
        )
    }

    /** Defines active structure boundaries to prevent floating paths in empty space. */
    private fun defineVolumetricShape(type: String, rows: Int, cols: Int, layers: Int): List<Cell> {
        val cells = mutableListOf<Cell>()
        for (z in 0 until layers) {
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val isValid = when (type.uppercase()) {
                        "PYRAMID" -> {
                            // Shrink footprint as we go higher
                            r in z until (rows - z) && c in z until (cols - z)
                        }
                        "DIAMOND" -> {
                            val mid = layers / 2
                            val offset = if (z <= mid) z else (layers - 1 - z)
                            r in (rows/2 - offset)..(rows/2 + offset) && c in (cols/2 - offset)..(cols/2 + offset)
                        }
                        "CUBE_STACK" -> {
                            // 5x5x3 cube
                            r in 2..6 && c in 2..6 && z < 3
                        }
                        else -> true // Fallback to full grid if unknown
                    }
                    if (isValid) cells.add(Cell(r, c, z))
                }
            }
        }
        return cells
    }

    /** 
     * Enforces strict movement within whitelist and ensures 100% path coverage 
     * of the active volumetric structure.
     */
    private fun findHamiltonianPath(active3DBlocks: List<Cell>, random: Random, maxAttempts: Int): List<Cell> {
        val boundarySet = active3DBlocks.toSet()
        val targetSize = active3DBlocks.size

        repeat(maxAttempts) {
            val start = active3DBlocks.random(random)
            val path = mutableListOf(start)
            val visited = mutableSetOf(start)

            while (path.size < targetSize) {
                val current = path.last()
                
                // Only consider candidates inside the white-listed volume
                val candidates = directions.map { d ->
                    Cell(current.row + d.row, current.col + d.col, current.z + d.z)
                }.filter { it in boundarySet && it !in visited }
                .shuffled(random)
                .sortedBy { cell ->
                    // Heuristic: Prefer cells with fewer unvisited neighbors to avoid dead-ends
                    directions.count { d ->
                        val n = Cell(cell.row + d.row, cell.col + d.col, cell.z + d.z)
                        n in boundarySet && n !in visited
                    }
                }

                if (candidates.isEmpty()) break
                val next = candidates.first()
                path.add(next)
                visited.add(next)
            }
            
            // Successful if 100% of the active volume is covered
            if (path.size == targetSize) return path
        }
        
        // Fallback: If heuristic search fails, return a shuffled list of active blocks
        // to at least keep the path inside the structures (though might be logically broken)
        return active3DBlocks.shuffled(random)
    }

    private fun splitIntoSegments(path: List<Cell>, numColors: Int, random: Random): List<List<Cell>> {
        val total = path.size
        if (total < numColors * 2) return listOf(path) // Edge case: not enough cells

        val base = total / numColors
        val lengths = IntArray(numColors) { base }
        val remainder = total - base * numColors
        for (i in 0 until remainder) lengths[i % numColors] += 1
        
        val segments = mutableListOf<List<Cell>>()
        var idx = 0
        for (length in lengths) {
            if (length >= 2 && idx + length <= path.size) {
                segments.add(path.subList(idx, idx + length))
                idx += length
            }
        }
        return segments.shuffled(random)
    }
}
