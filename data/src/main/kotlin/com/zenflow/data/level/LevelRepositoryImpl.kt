package com.zenflow.data.level

import com.zenflow.domain.model.Board
// 1. IMPORTAMOS LA INTERFAZ DESDE DOMAIN
import com.zenflow.domain.repository.LevelRepository

class LevelRepositoryImpl(
    private val dataSource: LevelDataSource
) : LevelRepository { // 2. AHORA IMPLEMENTA LA INTERFAZ CORRECTA QUE LA VISTA ESPERA

    override suspend fun getLevel(levelId: Int): Board {
        val dto = dataSource.loadLevel(levelId)
        return dto.toBoard()
    }

    override suspend fun getTotalLevels(): Int = dataSource.countLevels()
}
