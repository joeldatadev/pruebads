package com.zenflow.data.daily

/**
 * Ruta destino: data/src/main/kotlin/com/zenflow/data/daily/DailyChallengeRepositoryImpl.kt
 */
import com.zenflow.domain.model.DailyChallengeState
import com.zenflow.domain.repository.DailyChallengeRepository
import kotlinx.coroutines.flow.Flow

class DailyChallengeRepositoryImpl(
    private val dataStore: DailyChallengeDataStore
) : DailyChallengeRepository {

    override fun observeState(): Flow<DailyChallengeState> = dataStore.observeState()

    override suspend fun markCompleted(epochDay: Long) {
        dataStore.markCompleted(epochDay)
    }
}
