package com.hoshiraflow.domain.repository

/**
 * Ruta destino: domain/src/main/kotlin/com/zenflow/domain/repository/ProgressRepository.kt
 */
import kotlinx.coroutines.flow.Flow

interface ProgressRepository {
    fun observeCompletedLevels(): Flow<Set<Int>>
    suspend fun markLevelCompleted(levelId: Int)
    fun observeCompletedCubeLevels(): kotlinx.coroutines.flow.Flow<Set<Int>>
    suspend fun markCubeLevelCompleted(levelId: Int)
}



