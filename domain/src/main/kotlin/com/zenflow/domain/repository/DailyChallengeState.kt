package com.zenflow.domain.model

/**
 * Ruta destino: domain/src/main/kotlin/com/zenflow/domain/model/DailyChallengeState.kt
 */
data class DailyChallengeState(
    val lastCompletedEpochDay: Long? = null,
    val currentStreak: Int = 0
) {
    fun isCompletedOn(epochDay: Long): Boolean = lastCompletedEpochDay == epochDay
}
