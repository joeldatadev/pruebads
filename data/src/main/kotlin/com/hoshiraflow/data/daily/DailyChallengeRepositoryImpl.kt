package com.hoshiraflow.data.daily

import com.hoshiraflow.domain.repository.DailyChallengeRepository
import com.hoshiraflow.domain.model.DailyChallengeState
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import java.time.LocalDate
import java.time.LocalTime

class DailyChallengeRepositoryImpl(
    private val dataStore: DailyChallengeDataStore
) : DailyChallengeRepository {

    override fun observeState(): Flow<DailyChallengeState> = dataStore.observeState()

    override suspend fun markCompleted(epochDay: Long) {
        dataStore.markCompleted(epochDay)
    }

    /**
     * Calcula el epochDay del reto actual basándose en el corte de las 12:00 PM.
     */
    override fun getCurrentChallengeEpochDay(): Long {
        val now = LocalDateTime.now()
        val resetTime = LocalTime.of(12, 0)
        
        return if (now.toLocalTime().isBefore(resetTime)) {
            // Antes de las 12:00 PM, el reto es el del día anterior (que empezó ayer a las 12:00)
            now.toLocalDate().minusDays(1).toEpochDay()
        } else {
            // Después de las 12:00 PM, el reto es el de hoy
            now.toLocalDate().toEpochDay()
        }
    }

    override fun getNextResetTimeMillis(): Long {
        val now = LocalDateTime.now()
        val resetTime = LocalTime.of(12, 0)
        
        val nextReset = if (now.toLocalTime().isBefore(resetTime)) {
            LocalDateTime.of(now.toLocalDate(), resetTime)
        } else {
            LocalDateTime.of(now.toLocalDate().plusDays(1), resetTime)
        }
        
        return nextReset.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}



