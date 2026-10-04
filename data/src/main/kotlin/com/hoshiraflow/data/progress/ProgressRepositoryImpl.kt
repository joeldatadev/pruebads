package com.hoshiraflow.data.progress

/**
 * Ruta destino: data/src/main/kotlin/com/zenflow/data/progress/ProgressRepositoryImpl.kt
 */
import com.hoshiraflow.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.Flow

class ProgressRepositoryImpl(
    private val dataStore: ProgressDataStore
) : ProgressRepository {

    override fun observeCompletedLevels(): Flow<Set<Int>> = dataStore.observeCompletedLevels()

    override fun observeCompletedCubeLevels(): Flow<Set<Int>> = dataStore.observeCompletedCubeLevels()

    override fun observeCompletedSurfaceLevels(): Flow<Set<String>> = dataStore.observeCompletedSurfaceLevels()

    override suspend fun markLevelCompleted(levelId: Int) {
        dataStore.addCompletedLevel(levelId)
    }

    override suspend fun markCubeLevelCompleted(levelId: Int) {
        dataStore.addCompletedCubeLevel(levelId)
    }

    override suspend fun markSurfaceLevelCompleted(key: String) {
        dataStore.addCompletedSurfaceLevel(key)
    }

    override fun observeTimedBestScore(key: String): Flow<Int> = dataStore.observeTimedBestScore(key)

    override suspend fun saveTimedBestScoreIfHigher(key: String, score: Int) {
        dataStore.saveTimedBestScoreIfHigher(key, score)
    }
}
