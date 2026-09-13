package com.zenflow.data.level

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

class LevelDataSource(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun loadLevel(levelId: Int): LevelDto = withContext(Dispatchers.IO) {
        val fileName = "levels/level_%03d.json".format(levelId)
        val text = context.assets.open(fileName).bufferedReader().use { it.readText() }
        json.decodeFromString(LevelDto.serializer(), text)
    }

    suspend fun countLevels(): Int = withContext(Dispatchers.IO) {
        context.assets.list("levels")?.size ?: 0
    }
}
