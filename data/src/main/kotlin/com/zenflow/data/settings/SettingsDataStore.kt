package com.zenflow.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.zenflow.domain.repository.GameSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "zenflow_settings")

class SettingsDataStore(private val context: Context) {
    private val hapticsKey = booleanPreferencesKey("haptics_enabled")
    private val soundKey = booleanPreferencesKey("sound_enabled")

    fun observeSettings(): Flow<GameSettings> =
        context.settingsDataStore.data.map { prefs ->
            GameSettings(
                hapticsEnabled = prefs[hapticsKey] ?: true,
                soundEnabled = prefs[soundKey] ?: true
            )
        }

    suspend fun setHaptics(enabled: Boolean) {
        context.settingsDataStore.edit { it[hapticsKey] = enabled }
    }

    suspend fun setSound(enabled: Boolean) {
        context.settingsDataStore.edit { it[soundKey] = enabled }
    }
}