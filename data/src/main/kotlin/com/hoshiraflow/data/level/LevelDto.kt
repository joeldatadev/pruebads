package com.hoshiraflow.data.level

/**
 * Ruta destino: data/src/main/kotlin/com/zenflow/data/level/LevelDto.kt
 */
import kotlinx.serialization.Serializable

@Serializable
data class LevelDto(
    val id: Int,
    val rows: Int,
    val cols: Int,
    val nodes: List<NodeDto>,
    val topology: String? = null
)

@Serializable
data class NodeDto(
    val row: Int,
    val col: Int,
    val z: Int? = 0,
    val color: String,
    val pairId: Int? = null
)
