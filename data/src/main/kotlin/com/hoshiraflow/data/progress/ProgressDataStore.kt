package com.hoshiraflow.data.progress

/**
 * Ruta destino: data/src/main/kotlin/com/zenflow/data/progress/ProgressDataStore.kt
 */
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.progressDataStore by preferencesDataStore(name = "hoshiraflow_progress")

class ProgressDataStore(private val context: Context) {

    private val completedLevelsKey = stringSetPreferencesKey("completed_levels")
    private val completedCubeLevelsKey = stringSetPreferencesKey("completed_cube_levels")

    fun observeCompletedLevels(): Flow<Set<Int>> =
        context.progressDataStore.data.map { prefs ->
            prefs[completedLevelsKey]?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
        }

    fun observeCompletedCubeLevels(): Flow<Set<Int>> =
        context.progressDataStore.data.map { prefs ->
            prefs[completedCubeLevelsKey]?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
        }

    suspend fun addCompletedLevel(levelId: Int) {
        context.progressDataStore.edit { prefs ->
            val current = prefs[completedLevelsKey] ?: emptySet()
            prefs[completedLevelsKey] = current + levelId.toString()
        }
    }

    suspend fun addCompletedCubeLevel(levelId: Int) {
        context.progressDataStore.edit { prefs ->
            val current = prefs[completedCubeLevelsKey] ?: emptySet()
            prefs[completedCubeLevelsKey] = current + levelId.toString()
        }
    }
}



