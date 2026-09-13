package com.zenflow.domain.repository

/**
 * Ruta destino: domain/src/main/kotlin/com/zenflow/domain/repository/LevelRepository.kt
 */
import com.zenflow.domain.model.Board

interface LevelRepository {
    suspend fun getLevel(levelId: Int): Board
    suspend fun getTotalLevels(): Int
}
