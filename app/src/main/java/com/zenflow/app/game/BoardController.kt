package com.zenflow.app.game

import androidx.compose.ui.graphics.Color

data class Cell(val row: Int, val col: Int)

enum class GameColor(val color: Color) {
    RED(Color(0xFFE53935)),
    BLUE(Color(0xFF1E88E5)),
    GREEN(Color(0xFF43A047)),
    YELLOW(Color(0xFFFDD835)),
    ORANGE(Color(0xFFFB8C00)),
    PURPLE(Color(0xFF8E24AA))
}

data class Node(val cell: Cell, val color: GameColor)

data class Board(
    val rows: Int = 5,
    val cols: Int = 5,
    val nodes: List<Node> = emptyList(),
    val paths: Map<GameColor, List<Cell>> = emptyMap()
)