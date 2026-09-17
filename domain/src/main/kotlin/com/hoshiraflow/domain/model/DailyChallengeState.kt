package com.hoshiraflow.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class DailyChallengeState(
    val lastCompletedEpochDay: Long? = null,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0
) {
    /**
     * Verifica si el desafío fue completado en un día específico (Epoch Day).
     */
    fun isCompletedOn(epochDay: Long): Boolean = lastCompletedEpochDay == epochDay

    /**
     * Helper opcional para comprobar si está completado hoy.
     */
    fun isTodayCompleted(todayEpochDay: Long): Boolean = isCompletedOn(todayEpochDay)
}


