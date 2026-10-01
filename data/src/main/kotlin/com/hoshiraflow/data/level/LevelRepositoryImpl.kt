package com.hoshiraflow.data.level

import com.hoshiraflow.domain.model.Board
// 1. IMPORTAMOS LA INTERFAZ DESDE DOMAIN
import com.hoshiraflow.domain.model.PuzzleColor
import com.hoshiraflow.domain.model.SurfaceShapes
import com.hoshiraflow.domain.repository.LevelRepository
import com.hoshiraflow.domain.usecase.GenerateCubeLevelUseCase
import com.hoshiraflow.domain.usecase.GenerateSurfaceLevelUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LevelRepositoryImpl(
    private val dataSource: LevelDataSource
) : LevelRepository { // 2. AHORA IMPLEMENTA LA INTERFAZ CORRECTA QUE LA VISTA ESPERA

    private val generateCubeLevel = GenerateCubeLevelUseCase()
    private val generateSurfaceLevel = GenerateSurfaceLevelUseCase()

    override suspend fun getLevel(levelId: Int): Board {
        val dto = dataSource.loadLevel(levelId)
        return dto.toBoard()
    }

    override suspend fun getCubeLevel(levelId: Int): Board = withContext(Dispatchers.Default) {
        // Para el cubo usamos el generador procedural (100 niveles)
        val n = when {
            levelId <= 20 -> 3
            levelId <= 50 -> 4
            levelId <= 80 -> 5
            else -> 6
        }
        generateCubeLevel(seed = 12345L, index = levelId, n = n)
    }

    override suspend fun getTotalLevels(): Int = dataSource.countLevels()
    override suspend fun getTotalCubeLevels(): Int = 100

    override suspend fun getSurfaceLevel(shapeIndex: Int, levelId: Int): Board = withContext(Dispatchers.Default) {
        val models = SurfaceShapes.all
        val model = models[shapeIndex.coerceIn(0, models.lastIndex)]

        // El número de colores escala con las caras visibles del modelo (~1 color
        // cada 6 caras, es decir cada 2 caminos por voxel expuesto), acotado a la
        // paleta disponible y a un mínimo jugable de 3.
        val numColors = (model.faces.size / 6).coerceIn(3, PuzzleColor.entries.size)

        // Cada intento usa una semilla distinta y determinista: mismo shapeIndex +
        // levelId siempre produce el mismo tablero (importante para el reto diario
        // si algún día se reutiliza este generador ahí).
        val baseSeed = shapeIndex * 1_000_000L + levelId * 1_000L

        for (attempt in 0 until SURFACE_GENERATION_ATTEMPTS) {
            generateSurfaceLevel(model, seed = baseSeed + attempt, numColors = numColors)?.let { return@withContext it }
        }
        throw IllegalStateException(
            "No se pudo generar un nivel de superficie resoluble (shape=$shapeIndex, level=$levelId) " +
                "tras $SURFACE_GENERATION_ATTEMPTS intentos"
        )
    }

    override suspend fun getTotalSurfaceLevels(): Int = TOTAL_SURFACE_LEVELS_PER_SHAPE

    private companion object {
        const val SURFACE_GENERATION_ATTEMPTS = 40
        const val TOTAL_SURFACE_LEVELS_PER_SHAPE = 20
    }
}
