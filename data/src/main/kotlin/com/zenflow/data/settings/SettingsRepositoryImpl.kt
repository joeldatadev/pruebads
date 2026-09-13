package com.zenflow.data.settings

import com.zenflow.domain.repository.GameSettings
import com.zenflow.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

class SettingsRepositoryImpl(private val dataStore: SettingsDataStore) : SettingsRepository {
    override fun observeSettings(): Flow<GameSettings> = dataStore.observeSettings()
    override suspend fun setHaptics(enabled: Boolean) = dataStore.setHaptics(enabled)
    override suspend fun setSound(enabled: Boolean) = dataStore.setSound(enabled)
}