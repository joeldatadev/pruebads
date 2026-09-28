package com.hoshiraflow.domain.usecase

/**
 * Ruta destino: domain/src/main/kotlin/com/hoshiraflow/domain/usecase/GenerateSurfaceLevelUseCase.kt
 *
 * Requiere en Board:  val surface: SurfaceModel? = null
 * Requiere en BoardTopology:  SURFACE
 */
import com.hoshiraflow.domain.model.Board
import com.hoshiraflow.domain.model.BoardTopology
import com.hoshiraflow.domain.model.Cell
import com.hoshiraflow.domain.model.CellType
import com.hoshiraflow.domain.model.Node
import com.hoshiraflow.domain.model.PuzzleColor
import com.hoshiraflow.domain.model.SurfaceModel
import kotlin.random.Random

class GenerateSurfaceLevelUseCase {

    /** null si no encontró camino Hamiltoniano: reintenta con otro seed. */
    operator fun invoke(model: SurfaceModel, seed: Long, numColors: Int, maxAttempts: Int = 3000): Board? {
        val cells = model.faces.map { it.toCell() }
        if (cells.size < 4) return null
        val colors = numColors.coerceIn(1, cells.size / 2)
        val random = Random(seed)

        repeat(maxAttempts) {
            val path = hamiltonian(cells, model.neighbors, random) ?: return@repeat
            val nodes = split(path, colors, random).flatMapIndexed { i, seg ->
                val color = PuzzleColor.entries[i % PuzzleColor.entries.size]
                listOf(Node(seg.first(), color, i + 1), Node(seg.last(), color, i + 1))
            }
            return Board(
                rows = model.sizeY,
                cols = model.sizeX,
                nodes = nodes,
                cellTypes = cells.associateWith { CellType.Empty },
                topology = BoardTopology.SURFACE,
                surface = model
            )
        }
        return null
    }

    private fun hamiltonian(cells: List<Cell>, nb: Map<Cell, List<Cell>>, random: Random): List<Cell>? {
        // Empezar en una celda de grado bajo (esquinas) reduce los callejones sin salida.
        val low = cells.filter { (nb[it]?.size ?: 0) <= 2 }.ifEmpty { cells }
        val start = low.random(random)
        val visited = hashSetOf(start)
        val path = arrayListOf(start)
        while (path.size < cells.size) {
            val cur = path.last()
            val next = nb[cur].orEmpty()
                .filter { it !in visited }
                .shuffled(random)
                .minByOrNull { c -> nb[c].orEmpty().count { it !in visited } } // Warnsdorff
                ?: return null
            visited.add(next)
            path.add(next)
        }
        return path
    }

    private fun split(path: List<Cell>, n: Int, random: Random): List<List<Cell>> {
        val base = path.size / n
        val lengths = IntArray(n) { base }
        for (i in 0 until path.size - base * n) lengths[i % n]++
        var idx = 0
        return lengths.map { l -> path.subList(idx, idx + l).also { idx += l } }.shuffled(random)
    }
}
