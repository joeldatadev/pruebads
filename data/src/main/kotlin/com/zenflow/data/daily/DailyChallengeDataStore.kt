package com.zenflow.data.daily

/**
 * Ruta destino: data/src/main/kotlin/com/zenflow/data/daily/DailyChallengeDataStore.kt
 */
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.zenflow.domain.model.DailyChallengeState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dailyChallengeDataStore by preferencesDataStore(name = "zenflow_daily_challenge")

class DailyChallengeDataStore(private val context: Context) {

    private val lastCompletedEpochDayKey = longPreferencesKey("last_completed_epoch_day")
    private val streakKey = intPreferencesKey("current_streak")

    fun observeState(): Flow<DailyChallengeState> =
        context.dailyChallengeDataStore.data.map { prefs ->
            DailyChallengeState(
                lastCompletedEpochDay = prefs[lastCompletedEpochDayKey],
                currentStreak = prefs[streakKey] ?: 0
            )
        }

    suspend fun markCompleted(epochDay: Long) {
        context.dailyChallengeDataStore.edit { prefs ->
            val previous = prefs[lastCompletedEpochDayKey]
            val previousStreak = prefs[streakKey] ?: 0
            // La racha sigue solo si el último completado fue AYER (epochDay-1).
            // Si ya estaba completado HOY, no se duplica. Cualquier otro caso
            // (primera vez, o se saltó uno o más días) reinicia la racha en 1.
            val newStreak = when (previous) {
                epochDay -> previousStreak
                epochDay - 1 -> previousStreak + 1
                else -> 1
            }
            prefs[lastCompletedEpochDayKey] = epochDay
            prefs[streakKey] = newStreak
        }
    }
}
