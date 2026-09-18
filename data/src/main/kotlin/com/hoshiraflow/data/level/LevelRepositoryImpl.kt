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

    override suspend fun getCubeLevel(levelId: Int): Board {
        // Para el cubo usamos el generador procedural (100 niveles)
        val generator = com.hoshiraflow.domain.usecase.GenerateCubeLevelUseCase()
        val n = when {
            levelId <= 20 -> 3
            levelId <= 50 -> 4
            levelId <= 80 -> 5
            else -> 6
        }
        return generator(seed = 12345L, index = levelId, n = n)
    }

    override suspend fun getTotalLevels(): Int = dataSource.countLevels()
    override suspend fun getTotalCubeLevels(): Int = 100
}



