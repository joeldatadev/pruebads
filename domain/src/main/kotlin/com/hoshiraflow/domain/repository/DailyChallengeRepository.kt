package com.hoshiraflow.domain.repository

import com.hoshiraflow.domain.model.DailyChallengeState
import kotlinx.coroutines.flow.Flow

interface DailyChallengeRepository {
    /** Emite el estado (última fecha completada + racha) cada vez que cambia. */
    fun observeState(): Flow<DailyChallengeState>

    /** Marca el reto de [epochDay] como completado y actualiza la racha. */
    suspend fun markCompleted(epochDay: Long)

    /** Calcula el epochDay del reto actual basándose en el corte de las 12:00 PM. */
    fun getCurrentChallengeEpochDay(): Long

    /** Retorna el timestamp en ms del próximo reinicio (12:00 PM). */
    fun getNextResetTimeMillis(): Long
}



