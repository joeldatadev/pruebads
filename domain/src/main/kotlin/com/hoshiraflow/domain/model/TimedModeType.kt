package com.hoshiraflow.domain.model

/**
 * Ruta destino: domain/src/main/kotlin/com/hoshiraflow/domain/model/TimedModeType.kt
 */
enum class TimedDifficulty { FACIL, MEDIO, DIFICIL, EXTREMO }

sealed class TimedModeType {
    /** Contrarreloj: reloj fijo por dificultad, sin bonus de tiempo, sin Portales/Switch. */
    data class Resistencia(val difficulty: TimedDifficulty) : TimedModeType()

    /** Sprint 60: arranca en 45s, +tiempo por nivel resuelto, techo duro en 60s. */
    object Sprint60 : TimedModeType()

    /** Clave estable para guardar el mejor puntaje en ProgressRepository. */
    fun storageKey(): String = when (this) {
        is Resistencia -> "resistencia_${difficulty.name}"
        Sprint60 -> "sprint60"
    }
}
