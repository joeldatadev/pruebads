package com.hoshiraflow.data.level

import com.hoshiraflow.domain.model.Board
// 1. IMPORTAMOS LA INTERFAZ DESDE DOMAIN
import com.hoshiraflow.domain.repository.LevelRepository

class LevelRepositoryImpl(
    private val dataSource: LevelDataSource
) : LevelRepository { // 2. AHORA IMPLEMENTA LA INTERFAZ CORRECTA QUE LA VISTA ESPERA

    override suspend fun getLevel(levelId: Int): Board {
        val dto = dataSource.loadLevel(levelId)
        return dto.toBoard()
    }

    override suspend fun getIsometricLevel(levelId: Int): Board {
        val dto = dataSource.loadIsometricLevel(levelId)
        return dto.toBoard()
    }

    override suspend fun getTotalLevels(): Int = dataSource.countLevels()
    override suspend fun getTotalIsometricLevels(): Int = dataSource.countIsometricLevels()
}



