package com.zenflow.data.progress

/**
 * Ruta destino: data/src/main/kotlin/com/zenflow/data/progress/ProgressRepositoryImpl.kt
 */
import com.zenflow.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.Flow

class ProgressRepositoryImpl(
    private val dataStore: ProgressDataStore
) : ProgressRepository {

    override fun observeCompletedLevels(): Flow<Set<Int>> = dataStore.observeCompletedLevels()

    override suspend fun markLevelCompleted(levelId: Int) {
        dataStore.addCompletedLevel(levelId)
    }
}
