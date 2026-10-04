package com.hoshiraflow.domain.model

/**
 * Ruta destino: domain/src/main/kotlin/com/hoshiraflow/domain/model/SurfaceShape.kt
 *
 * Cualquier figura hecha de cubos (pirámide, escalera, zigzag...) se define como
 * un conjunto de voxels. Las celdas jugables son las CARAS VISIBLES (arriba,
 * izquierda, derecha) y dos caras son vecinas si comparten una arista 3D real.
 * Reemplaza la tabla manual de CubeEdgeMap: funciona para cualquier forma.
 *
 * Cell <-> cara:  Cell(row = x, col = y, z = vz * 3 + dir.ordinal)
 */

enum class FaceDir { TOP, LEFT, RIGHT } // LEFT = cara +y, RIGHT = cara +x

data class Voxel(val x: Int, val y: Int, val z: Int)

data class SurfaceFace(val x: Int, val y: Int, val z: Int, val dir: FaceDir) {

    fun toCell() = Cell(x, y, z * 3 + dir.ordinal)

    /** 4 vértices enteros (X, Y, Z) en orden cíclico. */
    fun vertices3D(): List<Triple<Int, Int, Int>> = when (dir) {
        FaceDir.TOP -> listOf(
            Triple(x, y, z + 1), Triple(x + 1, y, z + 1),
            Triple(x + 1, y + 1, z + 1), Triple(x, y + 1, z + 1)
        )
        FaceDir.RIGHT -> listOf(
            Triple(x + 1, y, z), Triple(x + 1, y + 1, z),
            Triple(x + 1, y + 1, z + 1), Triple(x + 1, y, z + 1)
        )
        FaceDir.LEFT -> listOf(
            Triple(x, y + 1, z), Triple(x + 1, y + 1, z),
            Triple(x + 1, y + 1, z + 1), Triple(x, y + 1, z + 1)
        )
    }

    companion object {
        fun fromCell(c: Cell) = SurfaceFace(c.row, c.col, c.z / 3, FaceDir.entries[c.z % 3])
    }
}

class SurfaceModel(val voxels: Set<Voxel>) {

    val faces: List<SurfaceFace>
    val neighbors: Map<Cell, List<Cell>>
    val sizeX: Int = (voxels.maxOfOrNull { it.x } ?: 0) + 1
    val sizeY: Int = (voxels.maxOfOrNull { it.y } ?: 0) + 1

    init {
        val maxDim = maxOf(
            voxels.maxOfOrNull { it.x } ?: 0,
            voxels.maxOfOrNull { it.y } ?: 0,
            voxels.maxOfOrNull { it.z } ?: 0
        ) + 1

        // La vista es (1,1,1): un voxel en (x+k, y+k, z+k) tapa exactamente al de (x,y,z).
        fun occluded(v: Voxel) = (1..maxDim).any { k -> Voxel(v.x + k, v.y + k, v.z + k) in voxels }

        faces = buildList {
            for (v in voxels) {
                if (occluded(v)) continue
                if (Voxel(v.x, v.y, v.z + 1) !in voxels) add(SurfaceFace(v.x, v.y, v.z, FaceDir.TOP))
                if (Voxel(v.x, v.y + 1, v.z) !in voxels) add(SurfaceFace(v.x, v.y, v.z, FaceDir.LEFT))
                if (Voxel(v.x + 1, v.y, v.z) !in voxels) add(SurfaceFace(v.x, v.y, v.z, FaceDir.RIGHT))
            }
        }.sortedWith(compareBy({ it.x + it.y + it.z }, { it.dir.ordinal }))

        // Arista = par de vértices; dos caras que la comparten son vecinas.
        fun key(t: Triple<Int, Int, Int>) = (t.first * 1000 + t.second) * 1000 + t.third
        val byEdge = HashMap<Pair<Long, Long>, MutableList<Cell>>()
        for (f in faces) {
            val vs = f.vertices3D().map { key(it).toLong() }
            for (i in vs.indices) {
                val a = vs[i]
                val b = vs[(i + 1) % vs.size]
                byEdge.getOrPut(minOf(a, b) to maxOf(a, b)) { mutableListOf() }.add(f.toCell())
            }
        }
        val nb = HashMap<Cell, MutableSet<Cell>>()
        faces.forEach { nb[it.toCell()] = linkedSetOf() }
        byEdge.values.filter { it.size >= 2 }.forEach { group ->
            for (a in group) for (b in group) if (a != b) nb.getValue(a).add(b)
        }
        neighbors = nb.mapValues { it.value.toList() }
    }

    fun isAdjacent(a: Cell, b: Cell): Boolean = b in neighbors[a].orEmpty()

    /** BFS corto (para cuando el dedo salta caras al arrastrar rápido). */
    fun pathBetween(from: Cell, to: Cell, maxLen: Int = 4): List<Cell> {
        if (from == to) return emptyList()
        val prev = HashMap<Cell, Cell>()
        var frontier = listOf(from)
        repeat(maxLen) {
            val next = mutableListOf<Cell>()
            for (c in frontier) for (n in neighbors[c].orEmpty()) {
                if (n == from || n in prev) continue
                prev[n] = c
                if (n == to) {
                    val out = ArrayDeque<Cell>()
                    var cur: Cell = to
                    while (cur != from) { out.addFirst(cur); cur = prev.getValue(cur) }
                    return out.toList()
                }
                next.add(n)
            }
            frontier = next
        }
        return emptyList()
    }
}

/** Proyección isométrica de vértices enteros (misma convención que IsometricProjection). */
object SurfaceProjection {
    private const val COS_30 = 0.8660254f
    private const val SIN_30 = 0.5f

    data class Layout(val originX: Float, val originY: Float, val cellSize: Float)

    fun project(X: Int, Y: Int, Z: Int, c: Float, ox: Float, oy: Float): Pair<Float, Float> =
        (ox + (X - Y) * c * COS_30) to (oy + (X + Y) * c * SIN_30 - Z * c)

    fun polygon(f: SurfaceFace, c: Float, ox: Float, oy: Float): List<Pair<Float, Float>> =
        f.vertices3D().map { (X, Y, Z) -> project(X, Y, Z, c, ox, oy) }

    fun fit(model: SurfaceModel, w: Float, h: Float, marginFraction: Float = 0.08f): Layout {
        if (model.faces.isEmpty() || w <= 0f || h <= 0f) return Layout(w / 2f, h / 2f, 0f)
        var minX = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
        for (f in model.faces) for ((x, y) in polygon(f, 1f, 0f, 0f)) {
            if (x < minX) minX = x; if (x > maxX) maxX = x
            if (y < minY) minY = y; if (y > maxY) maxY = y
        }
        val m = 1f - marginFraction
        val cs = minOf(w * m / (maxX - minX), h * m / (maxY - minY))
        return Layout(
            originX = (w - (maxX - minX) * cs) / 2f - minX * cs,
            originY = (h - (maxY - minY) * cs) / 2f - minY * cs,
            cellSize = cs
        )
    }
}

/** Formas listas. Cada carácter = altura de la columna ('.' o '0' = vacío). Fila = y, columna = x. */
object SurfaceShapes {

    fun fromHeightMap(rows: List<String>): SurfaceModel {
        val vs = mutableSetOf<Voxel>()
        rows.forEachIndexed { y, line ->
            line.forEachIndexed { x, ch ->
                val h = if (ch == '.') 0 else ch.digitToIntOrNull() ?: 0
                for (z in 0 until h) vs.add(Voxel(x, y, z))
            }
        }
        return SurfaceModel(vs)
    }

    fun hipPyramid(n: Int): SurfaceModel {
        val vs = mutableSetOf<Voxel>()
        for (y in 0 until n) {
            for (x in 0 until n) {
                val h = (n - 1 - maxOf(x, y)).coerceAtLeast(0)
                for (z in 0 until h) vs.add(Voxel(x, y, z))
            }
        }
        return SurfaceModel(vs)
    }

    fun diagonalRamp(n: Int): SurfaceModel {
        val vs = mutableSetOf<Voxel>()
        for (y in 0 until n) {
            for (x in 0 until n) {
                val h = (n - x - y).coerceAtLeast(0)
                for (z in 0 until h) vs.add(Voxel(x, y, z))
            }
        }
        return SurfaceModel(vs)
    }

    // Principio general: cualquier altura(x,y) que sea NO CRECIENTE al alejarse del
    // origen (0,0) tanto en x como en y por separado queda garantizada sin solapamientos
    // de caras (verificado con intersección de polígonos, 0 pares en las 4 formas de abajo).
    // Un pico central rodeado por varios lados (PYRAMID/ZIGZAG/TOWER viejos) sí se solapa.
    val STAIRS = fromHeightMap(listOf("4321", "3210", "2100", "1000"))
    val PYRAMID = fromHeightMap(listOf("43210", "33210", "22210", "11110", "00000"))
    val ZIGGURAT = fromHeightMap(listOf("22110", "22110", "11110", "11110", "00000"))
    val RAMP = fromHeightMap(listOf("43210000", "43210000", "43210000", "43210000"))
    // Pirámide con una esquina recortada a nivel 0 (silueta de "plaza con desnivel").
    val PLAZA = fromHeightMap(listOf("43210", "33210", "22210", "11100", "00000"))
    // Pico alargado: sube el doble de rápido en y que en x (cresta asimétrica, no un cono).
    val CRESTA = fromHeightMap(listOf("543210000", "333210000", "111110000", "000000000", "000000000"))
    // Mismos escalones que RAMP pero con anchos desiguales (1,2,1,2 celdas) en vez de parejos.
    val TERRAZAS = fromHeightMap(listOf("433211", "433211", "433211"))

    /**
     * Orden estable usado como `shapeIndex` en LevelRepository.getSurfaceLevel y en
     * SurfaceLevelSelectionScreen.SurfaceShapesList. Si agregas una forma nueva,
     * agrégala aquí Y a SurfaceShapesList en el mismo índice.
     */
    val all: List<SurfaceModel> = listOf(STAIRS, PYRAMID, ZIGGURAT, RAMP, PLAZA, CRESTA, TERRAZAS)
}
