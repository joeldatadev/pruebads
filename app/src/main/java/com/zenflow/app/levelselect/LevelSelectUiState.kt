package com.zenflow.app.levelselect

/**
 * Ruta destino: app/src/main/java/com/zenflow/app/levelselect/LevelSelectUiState.kt
 */
data class LevelSelectUiState(
    val isLoading: Boolean = true,
    val totalLevels: Int = 0,
    // TODO Fase 4: reemplazar por datos reales de ProgressRepository (estrellas, desbloqueo)
    val completedLevels: Set<Int> = emptySet(),
    val errorMessage: String? = null
)
