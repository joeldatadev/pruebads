package com.zenflow.app.game

/**
 * Ruta destino: app/src/main/kotlin/com/zenflow/app/game/GameUiState.kt
 */
import com.zenflow.domain.model.Board
import com.zenflow.domain.model.PuzzleColor

data class GameUiState(
    val isLoading: Boolean = true,
    val board: Board? = null,
    val activeColor: PuzzleColor? = null,       // color que se está arrastrando ahora mismo
    val connectedColors: Set<PuzzleColor> = emptySet(), // colores ya conectados (para "Snap" + partículas)
    val isLevelComplete: Boolean = false,
    val errorMessage: String? = null
)
