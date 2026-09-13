package com.zenflow.data.level

/**
 * Ruta destino: data/src/main/kotlin/com/zenflow/data/level/LevelMapper.kt
 */
import com.zenflow.domain.model.Board
import com.zenflow.domain.model.Cell
import com.zenflow.domain.model.Node
import com.zenflow.domain.model.PuzzleColor

fun LevelDto.toBoard(): Board {
    val nodes = nodes.map {
        Node(
            cell = Cell(it.row, it.col),
            color = PuzzleColor.valueOf(it.color.uppercase())
        )
    }
    return Board(rows = rows, cols = cols, nodes = nodes)
}
