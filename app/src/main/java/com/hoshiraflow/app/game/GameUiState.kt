package com.hoshiraflow.app.game

/**
 * Ruta destino: app/src/main/java/com/zenflow/app/game/GameUiState.kt (REEMPLAZA el archivo anterior)
 * Cambio: se agregó elapsedMs para mostrar el tiempo en la pantalla de celebración.
 */
import androidx.compose.runtime.Immutable
import com.hoshiraflow.domain.model.Board
import com.hoshiraflow.domain.model.PuzzleColor

@Immutable
data class GameUiState(
    val isLoading: Boolean = true,
    val board: Board? = null,
    val activeColor: PuzzleColor? = null,
    val connectedColors: Set<PuzzleColor> = emptySet(),
    val isLevelComplete: Boolean = false,
    val elapsedMs: Long? = null,
    val errorMessage: String? = null
)



