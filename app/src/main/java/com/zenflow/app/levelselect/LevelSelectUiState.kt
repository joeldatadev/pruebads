package com.zenflow.app.levelselect

/**
 * Ruta destino: app/src/main/java/com/zenflow/app/levelselect/LevelSelectUiState.kt
 */
data class LevelSelectUiState(
    val isLoading: Boolean = true,
    val totalLevels: Int = 0,
    val completedLevels: Set<Int> = emptySet(), // viene de ProgressRepository (DataStore)
    val dailyChallengeState: com.zenflow.domain.model.DailyChallengeState = com.zenflow.domain.model.DailyChallengeState(),
    val isDailyChallengeCompletedToday: Boolean = false,
    val errorMessage: String? = null
)
