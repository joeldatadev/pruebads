package com.hoshiraflow.data.settings

import com.hoshiraflow.domain.repository.GameSettings
import com.hoshiraflow.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

class SettingsRepositoryImpl(private val dataStore: SettingsDataStore) : SettingsRepository {
    override fun observeSettings(): Flow<GameSettings> = dataStore.observeSettings()
    override suspend fun setHaptics(enabled: Boolean) = dataStore.setHaptics(enabled)
    override suspend fun setSound(enabled: Boolean) = dataStore.setSound(enabled)
}


