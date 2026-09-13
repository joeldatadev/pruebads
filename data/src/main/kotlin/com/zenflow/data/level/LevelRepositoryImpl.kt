package com.zenflow.data.level

/**
 * Ruta destino: data/src/main/kotlin/com/zenflow/data/level/LevelRepositoryImpl.kt
 */
import com.zenflow.domain.model.Board
import com.zenflow.domain.repository.LevelRepository

class LevelRepositoryImpl(
    private val dataSource: LevelDataSource
) : LevelRepository {

    override suspend fun getLevel(levelId: Int): Board {
        val dto = dataSource.loadLevel(levelId)
        return dto.toBoard()
    }

    override suspend fun getTotalLevels(): Int = dataSource.countLevels()
}
