package com.hoshiraflow.data.daily

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hoshiraflow.domain.model.DailyChallengeState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dailyChallengeDataStore by preferencesDataStore(name = "hoshiraflow_daily_challenge")

class DailyChallengeDataStore(private val context: Context) {

    private val lastCompletedEpochDayKey = longPreferencesKey("last_completed_epoch_day")
    private val currentStreakKey = intPreferencesKey("current_streak")
    private val bestStreakKey = intPreferencesKey("best_streak")

    fun observeState(): Flow<DailyChallengeState> =
        context.dailyChallengeDataStore.data.map { prefs ->
            DailyChallengeState(
                lastCompletedEpochDay = prefs[lastCompletedEpochDayKey],
                currentStreak = prefs[currentStreakKey] ?: 0,
                bestStreak = prefs[bestStreakKey] ?: 0
            )
        }

    suspend fun markCompleted(epochDay: Long) {
        context.dailyChallengeDataStore.edit { prefs ->
            val previous = prefs[lastCompletedEpochDayKey]
            val currentStreak = prefs[currentStreakKey] ?: 0
            
            val newStreak = when (previous) {
                epochDay -> currentStreak // Already completed today
                epochDay - 1 -> currentStreak + 1
                else -> 1
            }
            
            val bestStreak = prefs[bestStreakKey] ?: 0
            if (newStreak > bestStreak) {
                prefs[bestStreakKey] = newStreak
            }
            
            prefs[lastCompletedEpochDayKey] = epochDay
            prefs[currentStreakKey] = newStreak
        }
    }
}



