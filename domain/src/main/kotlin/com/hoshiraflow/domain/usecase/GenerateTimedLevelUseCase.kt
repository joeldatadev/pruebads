package com.hoshiraflow.domain.usecase

/**
 * Ruta destino: domain/src/main/kotlin/com/hoshiraflow/domain/usecase/GenerateTimedLevelUseCase.kt
 *
 * Centraliza "qué generador toca ahora" para los modos cronometrados, para no
 * repetir esa lógica en Resistencia y Sprint 60.
 *
 * IMPORTANTE: nunca usa GenerateMasterLevelUseCase. Modo Maestro combina Portales
 * y Switch por diseño (ver GenerateMasterLevelUseCase), y ambas mecánicas quedan
 * explícitamente fuera de los modos cronometrados — por eso tampoco se usan
 * GeneratePortalLevelUseCase ni GenerateSwitchLevelUseCase directamente.
 *
 * Los tableros de Cubo usan n=3 (la cara más chica que genera GenerateCubeLevelUseCase,
 * 3 colores) para que no se vuelvan lentos de resolver bajo presión de tiempo.
 */
import com.hoshiraflow.domain.model.Board
import com.hoshiraflow.domain.model.SurfaceShapes
import com.hoshiraflow.domain.model.TimedDifficulty
import com.hoshiraflow.domain.model.TimedModeType
import kotlin.random.Random

class GenerateTimedLevelUseCase(
    private val generateProcedural: GenerateProceduralLevelUseCase = GenerateProceduralLevelUseCase(),
    private val generateCube: GenerateCubeLevelUseCase = GenerateCubeLevelUseCase(),
    private val generateSurface: GenerateSurfaceLevelUseCase = GenerateSurfaceLevelUseCase()
) {
    data class TimedLevel(val board: Board, val timeBonusMs: Long)

    fun next(type: TimedModeType, elapsedSeconds: Int, levelsSolved: Int, random: Random): TimedLevel =
        when (type) {
            is TimedModeType.Resistencia -> forResistencia(type.difficulty, random)
            TimedModeType.Sprint60 -> forSprint(elapsedSeconds, random)
        }

    /** Resistencia: reloj fijo, sin bonus. La dificultad define el rango de generadores. */
    private fun forResistencia(difficulty: TimedDifficulty, random: Random): TimedLevel {
        val board = when (difficulty) {
            TimedDifficulty.FACIL ->
                generateProcedural(seed = random.nextLong(), index = random.nextInt(1, 16))
            TimedDifficulty.MEDIO ->
                generateProcedural(seed = random.nextLong(), index = random.nextInt(25, 56))
            TimedDifficulty.DIFICIL ->
                smallCube(random)
            TimedDifficulty.EXTREMO -> pickOne(
                random,
                { smallCube(random) },
                { generateSurfaceOrFallback(random) }
            )
        }
        return TimedLevel(board, timeBonusMs = 0L)
    }

    /** Sprint 60: dificultad escala con el tiempo transcurrido; cada tramo da su propio bonus. */
    private fun forSprint(elapsedSeconds: Int, random: Random): TimedLevel = when {
        elapsedSeconds < 20 -> TimedLevel(
            generateProcedural(seed = random.nextLong(), index = random.nextInt(1, 16)),
            timeBonusMs = 4_000L
        )
        elapsedSeconds < 40 -> TimedLevel(
            generateProcedural(seed = random.nextLong(), index = random.nextInt(25, 56)),
            timeBonusMs = 7_000L
        )
        else -> TimedLevel(
            pickOne(
                random,
                { smallCube(random) },
                { generateSurfaceOrFallback(random) }
            ),
            timeBonusMs = 10_000L
        )
    }

    private fun smallCube(random: Random): Board =
        generateCube(seed = random.nextLong(), index = random.nextInt(1, 20), n = 3)

    private fun generateSurfaceOrFallback(random: Random): Board {
        val model = if (random.nextBoolean()) SurfaceShapes.hipPyramid(5) else SurfaceShapes.diagonalRamp(5)
        val baseSeed = random.nextLong()
        repeat(20) { attempt ->
            generateSurface(model, seed = baseSeed + attempt, numColors = 6)?.let { return it }
        }
        // Fallback de seguridad: no debería pasar nunca (ambas formas están verificadas
        // sin solapamientos), pero evita dejar al jugador sin tablero si algo falla.
        return generateProcedural(seed = baseSeed, index = 10)
    }

    private fun pickOne(random: Random, vararg options: () -> Board): Board =
        options[random.nextInt(options.size)]()
}
