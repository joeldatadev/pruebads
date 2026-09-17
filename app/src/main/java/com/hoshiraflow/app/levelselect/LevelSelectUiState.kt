package com.hoshiraflow.app.levelselect

import androidx.compose.runtime.Immutable
import com.hoshiraflow.domain.model.DailyChallengeState

/**
 * Ruta destino: app/src/main/java/com/zenflow/app/levelselect/LevelSelectUiState.kt
 */
@Immutable
data class LevelSelectUiState(
    val isLoading: Boolean = true,
    val totalLevels: Int = 0,
    val completedLevels: Set<Int> = emptySet(), // viene de ProgressRepository (DataStore)
    val dailyChallengeState: DailyChallengeState = DailyChallengeState(),
    val isDailyChallengeCompletedToday: Boolean = false,
    val dailyChallengeCountdown: String = "",
    val completedCubeLevels: Set<Int> = emptySet(),
    val totalCubeLevels: Int = 0,
    val errorMessage: String? = null
)



