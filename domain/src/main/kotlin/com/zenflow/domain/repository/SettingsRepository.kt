package com.zenflow.domain.repository

import kotlinx.coroutines.flow.Flow

data class GameSettings(
    val hapticsEnabled: Boolean = true,
    val soundEnabled: Boolean = true
)

interface SettingsRepository {
    fun observeSettings(): Flow<GameSettings>
    suspend fun setHaptics(enabled: Boolean)
    suspend fun setSound(enabled: Boolean)
}