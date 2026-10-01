package com.hoshiraflow.domain.repository

/**
 * Ruta destino: domain/src/main/kotlin/com/zenflow/domain/repository/LevelRepository.kt
 */
import com.hoshiraflow.domain.model.Board

interface LevelRepository {
    suspend fun getLevel(levelId: Int): Board
    suspend fun getCubeLevel(levelId: Int): Board
    suspend fun getTotalLevels(): Int
    suspend fun getTotalCubeLevels(): Int

    /** shapeIndex referencia SurfaceShapes.all (0=Pirámide, 1=Escalera, 2=Zigzag, 3=Torre; mismo orden que SurfaceShapesList en la UI). */
    suspend fun getSurfaceLevel(shapeIndex: Int, levelId: Int): Board
    suspend fun getTotalSurfaceLevels(): Int
}
