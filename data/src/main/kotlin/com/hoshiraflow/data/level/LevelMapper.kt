package com.hoshiraflow.data.level

/**
 * Ruta destino: data/src/main/kotlin/com/zenflow/data/level/LevelMapper.kt
 */
import com.hoshiraflow.domain.model.Board
import com.hoshiraflow.domain.model.BoardTopology
import com.hoshiraflow.domain.model.Cell
import com.hoshiraflow.domain.model.Node
import com.hoshiraflow.domain.model.PuzzleColor

fun LevelDto.toBoard(): Board {
    val nodes = nodes.mapIndexed { index, it ->
        Node(
            cell = Cell(it.row, it.col, it.z ?: 0),
            color = PuzzleColor.valueOf(it.color.uppercase()),
            pairId = it.pairId ?: (index / 2 + 1)
        )
    }
    val boardTopology = when(topology?.uppercase()) {
        "ISOMETRIC" -> BoardTopology.ISOMETRIC
        else -> BoardTopology.CARTESIAN
    }
    return Board(rows = rows, cols = cols, nodes = nodes, topology = boardTopology)
}
