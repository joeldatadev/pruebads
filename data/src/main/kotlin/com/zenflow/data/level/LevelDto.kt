package com.zenflow.data.level

/**
 * Ruta destino: data/src/main/kotlin/com/zenflow/data/level/LevelDto.kt
 *
 * Formato JSON esperado (ejemplo en assets/levels/level_001.json):
 * {
 *   "id": 1,
 *   "rows": 5,
 *   "cols": 5,
 *   "nodes": [
 *     { "row": 0, "col": 0, "color": "RED" },
 *     { "row": 4, "col": 4, "color": "RED" },
 *     { "row": 0, "col": 4, "color": "BLUE" },
 *     { "row": 4, "col": 0, "color": "BLUE" }
 *   ]
 * }
 */
import kotlinx.serialization.Serializable

@Serializable
data class LevelDto(
    val id: Int,
    val rows: Int,
    val cols: Int,
    val nodes: List<NodeDto>
)

@Serializable
data class NodeDto(
    val row: Int,
    val col: Int,
    val color: String
)
