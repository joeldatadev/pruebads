package com.hoshiraflow.domain.repository

/**
 * Ruta destino: domain/src/main/kotlin/com/zenflow/domain/repository/LevelRepository.kt
 */
import com.hoshiraflow.domain.model.Board

interface LevelRepository {
    suspend fun getLevel(levelId: Int): Board
    suspend fun getIsometricLevel(levelId: Int): Board
    suspend fun getTotalLevels(): Int
    suspend fun getTotalIsometricLevels(): Int
}



