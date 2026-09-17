package com.hoshiraflow.data.level

/**
 * Ruta destino: data/src/main/kotlin/com/zenflow/data/level/LevelDataSource.kt
 *
 * Lee JSON desde app/src/main/assets/levels/level_XXX.json
 * Nota: :data necesita Context, por eso este módulo es Android library (no jvm puro).
 */
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer

class LevelDataSource(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun loadLevel(levelId: Int): LevelDto = withContext(Dispatchers.IO) {
        val fileName = "levels/level_%03d.json".format(levelId)
        val text = context.assets.open(fileName).bufferedReader().use { it.readText() }
        json.decodeFromString(LevelDto.serializer(), text)
    }

    suspend fun loadIsometricLevel(levelId: Int): LevelDto = withContext(Dispatchers.IO) {
        val text = context.assets.open("levels_3d.json").bufferedReader().use { it.readText() }
        val levels = json.decodeFromString(ListSerializer(LevelDto.serializer()), text)
        levels.firstOrNull { it.id == levelId } ?: levels.first()
    }

    suspend fun countLevels(): Int = withContext(Dispatchers.IO) {
        context.assets.list("levels")?.size ?: 0
    }

    suspend fun countIsometricLevels(): Int = withContext(Dispatchers.IO) {
        val text = context.assets.open("levels_3d.json").bufferedReader().use { it.readText() }
        val levels = json.decodeFromString(ListSerializer(LevelDto.serializer()), text)
        levels.size
    }
}



