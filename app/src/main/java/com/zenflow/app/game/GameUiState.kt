package com.zenflow.app.game

/**
 * Ruta destino: app/src/main/java/com/zenflow/app/game/GameUiState.kt (REEMPLAZA el archivo anterior)
 * Cambio: se agregó elapsedMs para mostrar el tiempo en la pantalla de celebración.
 */
import com.zenflow.domain.model.Board
import com.zenflow.domain.model.PuzzleColor

data class GameUiState(
    val isLoading: Boolean = true,
    val board: Board? = null,
    val activeColor: PuzzleColor? = null,
    val connectedColors: Set<PuzzleColor> = emptySet(),
    val isLevelComplete: Boolean = false,
    val elapsedMs: Long? = null,
    val errorMessage: String? = null
)
