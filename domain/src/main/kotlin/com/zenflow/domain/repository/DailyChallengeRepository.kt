package com.zenflow.domain.repository

/**
 * Ruta destino: domain/src/main/kotlin/com/zenflow/domain/repository/DailyChallengeRepository.kt
 */
import com.zenflow.domain.model.DailyChallengeState
import kotlinx.coroutines.flow.Flow

interface DailyChallengeRepository {
    /** Emite el estado (última fecha completada + racha) cada vez que cambia. */
    fun observeState(): Flow<DailyChallengeState>

    /** Marca el reto de [epochDay] como completado y actualiza la racha. */
    suspend fun markCompleted(epochDay: Long)
}
