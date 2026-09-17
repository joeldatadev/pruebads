package com.hoshiraflow.domain.util

import com.hoshiraflow.domain.model.Board
import com.hoshiraflow.domain.model.Cell
import kotlin.math.roundToInt

/**
 * Utility for Isometric Projection math.
 * Standard isometric angle is 30 degrees. 
 */
object IsometricProjection {

    private const val COS_30 = 0.8660254f
    private const val SIN_30 = 0.5f

    /**
     * Converts grid (col, row, heightZ) to Screen Offset.
     */
    fun gridToScreen(
        col: Int,
        row: Int,
        heightZ: Float,
        cellWidth: Float,
        cellHeight: Float,
        originX: Float = 0f,
        originY: Float = 0f,
        layerHeight: Float = 40f
    ): Pair<Float, Float> {
        val screenX = originX + (col - row) * (cellWidth * COS_30)
        val screenY = originY + (col + row) * (cellHeight * SIN_30) - (heightZ * layerHeight)
        return screenX to screenY
    }

    /**
     * Top-Down Z-Ordered Face Picker.
     * Iterates through active 3D blocks from highest to lowest Z to detect touch intersection.
     */
    fun getHitBlockFace(
        touchX: Float,
        touchY: Float,
        board: Board,
        cellWidth: Float,
        cellHeight: Float,
        originX: Float,
        originY: Float,
        layerHeight: Float
    ): Cell? {
        val allCells = board.cellTypes.keys + board.nodes.map { it.cell } + board.paths.values.flatten()
        val sortedCells = allCells.distinct().sortedByDescending { it.z }

        for (cell in sortedCells) {
            val screenPos = gridToScreen(cell.col, cell.row, cell.z.toFloat(), cellWidth, cellHeight, originX, originY, layerHeight)
            val cx = screenPos.first
            val cy = screenPos.second
            
            val w = cellWidth * COS_30
            val h = cellHeight * SIN_30

            // 1. Check Top Rhombus Face
            val topPolygonX = floatArrayOf(cx, cx + w, cx, cx - w)
            val topPolygonY = floatArrayOf(cy - h, cy, cy + h, cy)
            if (isPointInPolygon(touchX, touchY, topPolygonX, topPolygonY)) return cell

            // 2. Check Left Wall Face
            val leftPolygonX = floatArrayOf(cx - w, cx, cx, cx - w)
            val leftPolygonY = floatArrayOf(cy, cy + h, cy + h + layerHeight, cy + layerHeight)
            if (isPointInPolygon(touchX, touchY, leftPolygonX, leftPolygonY)) return cell

            // 3. Check Right Wall Face
            val rightPolygonX = floatArrayOf(cx + w, cx, cx, cx + w)
            val rightPolygonY = floatArrayOf(cy, cy + h, cy + h + layerHeight, cy + layerHeight)
            if (isPointInPolygon(touchX, touchY, rightPolygonX, rightPolygonY)) return cell
        }
        return null
    }

    private fun isPointInPolygon(px: Float, py: Float, polyX: FloatArray, polyY: FloatArray): Boolean {
        var collision = false
        var next: Int
        for (current in polyX.indices) {
            next = current + 1
            if (next == polyX.size) next = 0
            if (((polyY[current] > py) != (polyY[next] > py)) && 
                (px < (polyX[next] - polyX[current]) * (py - polyY[current]) / (polyY[next] - polyY[current]) + polyX[current])) {
                collision = !collision
            }
        }
        return collision
    }

    /**
     * Converts Screen Offset back to nearest Grid (col, row).
     */
    fun screenToGrid(
        x: Float,
        y: Float,
        cellWidth: Float,
        cellHeight: Float,
        originX: Float = 0f,
        originY: Float = 0f,
        layerHeight: Float = 40f
    ): Cell {
        val relX = x - originX
        val relY = y - originY
        
        val hFactor = cellHeight * SIN_30
        val wFactor = cellWidth * COS_30

        val colMinusRow = relX / wFactor
        val colPlusRow = relY / hFactor
        
        val col = (colPlusRow + colMinusRow) / 2f
        val row = (colPlusRow - colMinusRow) / 2f
        
        return Cell(row.roundToInt(), col.roundToInt())
    }
}
