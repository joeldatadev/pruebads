package com.zenflow.data.progress

/**
 * Ruta destino: data/src/main/kotlin/com/zenflow/data/progress/ProgressDataStore.kt
 */
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.progressDataStore by preferencesDataStore(name = "zenflow_progress")

class ProgressDataStore(private val context: Context) {

    private val completedLevelsKey = stringSetPreferencesKey("completed_levels")

    fun observeCompletedLevels(): Flow<Set<Int>> =
        context.progressDataStore.data.map { prefs ->
            prefs[completedLevelsKey]?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
        }

    suspend fun addCompletedLevel(levelId: Int) {
        context.progressDataStore.edit { prefs ->
            val current = prefs[completedLevelsKey] ?: emptySet()
            prefs[completedLevelsKey] = current + levelId.toString()
        }
    }
}
