package com.zenflow.domain.repository

/**
 * Ruta destino: domain/src/main/kotlin/com/zenflow/domain/repository/ProgressRepository.kt
 */
import kotlinx.coroutines.flow.Flow

interface ProgressRepository {
    fun observeCompletedLevels(): Flow<Set<Int>>
    suspend fun markLevelCompleted(levelId: Int)
}
