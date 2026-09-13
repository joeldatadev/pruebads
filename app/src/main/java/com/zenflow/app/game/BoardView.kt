package com.zenflow.app.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun BoardView(
    board: Board,
    onBoardChanged: (Board) -> Unit,
    modifier: Modifier = Modifier
) {
    val controller = BoardController(board)

    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(1f)
            .fillMaxSize()
    ) {
        val boardSizePx = constraints.maxWidth.toFloat()
        val cellSizePx = boardSizePx / board.cols

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startCell = offsetToCell(down.position, cellSizePx, board.rows, board.cols)

                        if (startCell != null) {
                            controller.onTouchStart(startCell)
                            onBoardChanged(controller.board)
                        }

                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            change.consume()

                            val currentCell = offsetToCell(change.position, cellSizePx, board.rows, board.cols)
                            if (currentCell != null) {
                                controller.onTouchMove(currentCell)
                                onBoardChanged(controller.board)
                            }
                        } while (event.changes.any { it.pressed })

                        controller.onTouchEnd()
                        onBoardChanged(controller.board)
                    }
                }
        ) {
            // 1. Cuadrícula limpia
            drawGridLines(board.rows, board.cols, cellSizePx)

            // 2. Trazo de caminos
            drawPaths(board, cellSizePx)

            // 3. Nodos de colores
            drawNodes(board.nodes, cellSizePx)
        }
    }
}

private fun offsetToCell(offset: Offset, cellSizePx: Float, rows: Int, cols: Int): Cell? {
    if (cellSizePx <= 0f) return null
    val col = (offset.x / cellSizePx).toInt()
    val row = (offset.y / cellSizePx).toInt()
    return if (row in 0 until rows && col in 0 until cols) {
        Cell(row, col)
    } else null
}

private fun DrawScope.drawGridLines(rows: Int, cols: Int, cellSizePx: Float) {
    val lineColor = Color.LightGray.copy(alpha = 0.5f)
    val strokeWidth = 2f

    for (i in 0..rows) {
        val y = i * cellSizePx
        drawLine(
            color = lineColor,
            start = Offset(0f, y),
            end = Offset(cols * cellSizePx, y),
            strokeWidth = strokeWidth
        )
    }

    for (j in 0..cols) {
        val x = j * cellSizePx
        drawLine(
            color = lineColor,
            start = Offset(x, 0f),
            end = Offset(x, rows * cellSizePx),
            strokeWidth = strokeWidth
        )
    }
}

private fun DrawScope.drawPaths(board: Board, cellSizePx: Float) {
    val pathStrokeWidth = cellSizePx * 0.32f

    board.paths.forEach { (gameColor, cells) ->
        if (cells.isEmpty()) return@forEach

        val path = Path()
        val firstCenter = getCellCenter(cells.first(), cellSizePx)
        path.moveTo(firstCenter.x, firstCenter.y)

        for (i in 1 until cells.size) {
            val center = getCellCenter(cells[i], cellSizePx)
            path.lineTo(center.x, center.y)
        }

        // Resplandor discreto ajustado (Sombra limpia, sin blur excesivo)
        drawPath(
            path = path,
            color = gameColor.color.copy(alpha = 0.25f),
            style = Stroke(
                width = pathStrokeWidth * 1.4f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Trazo principal nítido
        drawPath(
            path = path,
            color = gameColor.color,
            style = Stroke(
                width = pathStrokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

private fun DrawScope.drawNodes(nodes: List<Node>, cellSizePx: Float) {
    val nodeRadius = cellSizePx * 0.30f

    nodes.forEach { node ->
        val center = getCellCenter(node.cell, cellSizePx)
        drawCircle(
            color = node.color.color,
            radius = nodeRadius,
            center = center
        )
    }
}

private fun getCellCenter(cell: Cell, cellSizePx: Float): Offset {
    return Offset(
        x = (cell.col + 0.5f) * cellSizePx,
        y = (cell.row + 0.5f) * cellSizePx
    )
}