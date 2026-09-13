package com.zenflow.data.level

import com.zenflow.domain.model.Board

class LevelRepositoryImpl(
    private val dataSource: LevelDataSource
) : LevelRepository {

    override suspend fun getLevel(levelId: Int): Board {
        val dto = dataSource.loadLevel(levelId)
        return dto.toBoard()
    }

    override suspend fun getTotalLevels(): Int = dataSource.countLevels()
}