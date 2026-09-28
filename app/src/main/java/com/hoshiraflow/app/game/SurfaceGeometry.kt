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
import com.hoshiraflow.domain.model.Cell
import com.hoshiraflow.domain.model.FaceDir
import com.hoshiraflow.domain.model.SurfaceModel
import com.hoshiraflow.domain.model.SurfaceProjection

class SurfaceGeometry(val model: SurfaceModel, width: Float, height: Float) {

    val layout = SurfaceProjection.fit(model, width, height)
    val cellSize get() = layout.cellSize

    private val polys = model.faces.map {
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

    /** Las caras visibles no se solapan en pantalla, así que el orden no importa. */
    fun hit(p: Offset): Cell? {
        for (i in polys.indices) if (inside(polys[i], p)) return model.faces[i].toCell()
        return null
    }

    fun DrawScope.drawSurface() {
        val edge = Color(0xFF3A4360)
        model.faces.forEachIndexed { i, f ->
            // 3 tonos = volumen sin ruido visual; la luz "cae" desde arriba.
            val fill = when (f.dir) {
                FaceDir.TOP -> Color(0xFF2A3145)
                FaceDir.RIGHT -> Color(0xFF1E2436)
                FaceDir.LEFT -> Color(0xFF151A28)
            }
            drawPath(paths[i], fill)
            drawPath(paths[i], edge, style = Stroke(width = 1.5f))
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
