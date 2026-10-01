package com.hoshiraflow.domain.repository

/**
 * Ruta destino: domain/src/main/kotlin/com/zenflow/domain/repository/ProgressRepository.kt
 */
import kotlinx.coroutines.flow.Flow

interface ProgressRepository {
    fun observeCompletedLevels(): Flow<Set<Int>>
    suspend fun markLevelCompleted(levelId: Int)
    fun observeCompletedCubeLevels(): Flow<Set<Int>>
    suspend fun markCubeLevelCompleted(levelId: Int)

    /** Claves con formato "${shapeIndex}_${levelId}" (mismo formato que usa SurfaceLevelSelectionScreen). */
    fun observeCompletedSurfaceLevels(): Flow<Set<String>>
    suspend fun markSurfaceLevelCompleted(key: String)
}
