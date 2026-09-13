package com.zenflow.domain.usecase

/**
 * Ruta destino: domain/src/main/kotlin/com/zenflow/domain/usecase/GetDailyChallengeSeedUseCase.kt
 *
 * Reto Diario: mismo tablero para todos los jugadores en la misma fecha.
 * Reusa GenerateProceduralLevelUseCase(seed, index) -> Board (el mismo
 * generador del Modo Infinito) en vez de mantener un segundo generador.
 * El índice de dificultad queda FIJO en un punto intermedio: el reto diario
 * no tiene que escalar como el Modo Infinito, solo ser el mismo desafío
 * para todos ese día.
 */
import java.time.LocalDate

class GetDailyChallengeSeedUseCase {
    operator fun invoke(date: LocalDate = LocalDate.now()): DailyChallengeSeed {
        val epochDay = date.toEpochDay()
        return DailyChallengeSeed(seed = epochDay, index = DAILY_DIFFICULTY_INDEX, epochDay = epochDay)
    }

    companion object {
        // index=45 -> 6x6, 6 colores en GenerateProceduralLevelUseCase.difficultyFor().
        // Ni tan fácil que no rete a un jugador con experiencia, ni tan grande
        // que desanime a quien solo tiene un minuto libre.
        private const val DAILY_DIFFICULTY_INDEX = 45
    }
}

data class DailyChallengeSeed(val seed: Long, val index: Int, val epochDay: Long)
