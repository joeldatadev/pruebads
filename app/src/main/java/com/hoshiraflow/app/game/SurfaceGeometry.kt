package com.hoshiraflow.app.game

/**
 * Ruta destino: app/src/main/java/com/hoshiraflow/app/game/SurfaceGeometry.kt
 *
 * Se construye UNA vez por (modelo, tamaño) con remember -> cero Path() ni
 * proyecciones dentro del DrawScope.
 */
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.hoshiraflow.domain.model.Board
import com.hoshiraflow.domain.model.Cell
import com.hoshiraflow.domain.model.FaceDir
import com.hoshiraflow.domain.model.SurfaceModel
import com.hoshiraflow.domain.model.SurfaceProjection

class SurfaceGeometry(val model: SurfaceModel, width: Float, height: Float) {

    val layout = SurfaceProjection.fit(model, width, height)
    val cellSize get() = layout.cellSize

    val polys = model.faces.map {
        SurfaceProjection.polygon(it, layout.cellSize, layout.originX, layout.originY)
    }

    private val paths: List<Path> = polys.map { poly ->
        Path().apply {
            poly.forEachIndexed { i, p -> if (i == 0) moveTo(p.first, p.second) else lineTo(p.first, p.second) }
            close()
        }
    }

    val centers: Map<Cell, Offset> = model.faces.mapIndexed { i, f ->
        f.toCell() to Offset(polys[i].map { it.first }.average().toFloat(), polys[i].map { it.second }.average().toFloat())
    }.toMap()

    /** Búsqueda de frente a atrás para dar prioridad a las caras frontales visibles. */
    fun hit(p: Offset): Cell? {
        for (i in polys.indices.reversed()) {
            if (inside(polys[i], p)) return model.faces[i].toCell()
        }

        // Tolerancia secundaria para gestos rápidos en proximidad al centroide
        val thresholdSq = (cellSize * 0.55f) * (cellSize * 0.55f)
        var closestCell: Cell? = null
        var minDistSq = Float.MAX_VALUE
        for (i in polys.indices.reversed()) {
            val cell = model.faces[i].toCell()
            val center = centers[cell] ?: continue
            val dx = p.x - center.x
            val dy = p.y - center.y
            val distSq = dx * dx + dy * dy
            if (distSq <= thresholdSq && distSq < minDistSq) {
                minDistSq = distSq
                closestCell = cell
            }
        }
        return closestCell
    }

    fun DrawScope.drawSurface(board: Board? = null) {
        val strokeColor = Color.White.copy(alpha = 0.35f)
        val centerDotColor = Color.White.copy(alpha = 0.25f)

        model.faces.forEachIndexed { i, f ->
            val cell = f.toCell()
            val fill = when (f.dir) {
                FaceDir.TOP -> Color(0xFF2E3B4E)
                FaceDir.RIGHT -> Color(0xFF232D3D)
                FaceDir.LEFT -> Color(0xFF1A222F)
            }
            drawPath(paths[i], fill)
            drawPath(paths[i], strokeColor, style = Stroke(width = 2.dp.toPx()))

            // Guía discreta en el centroide de casillas vacías para definir bien la cuadrícula 3D
            val hasNode = board?.nodes?.any { it.cell == cell } == true
            if (!hasNode) {
                centers[cell]?.let { center ->
                    drawCircle(
                        color = centerDotColor,
                        radius = (cellSize * 0.08f).coerceIn(2.5f.dp.toPx(), 4.5f.dp.toPx()),
                        center = center
                    )
                }
            }
        }
    }

    private fun inside(poly: List<Pair<Float, Float>>, p: Offset): Boolean {
        var c = false
        var j = poly.size - 1
        for (i in poly.indices) {
            val (xi, yi) = poly[i]
            val (xj, yj) = poly[j]
            if ((yi > p.y) != (yj > p.y) && p.x < (xj - xi) * (p.y - yi) / (yj - yi) + xi) c = !c
            j = i
        }
        return c
    }
}
