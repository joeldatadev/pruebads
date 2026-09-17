package com.hoshiraflow.app.game

/**
 * Ruta destino: app/src/main/kotlin/com/zenflow/app/game/GameEvent.kt
 *
 * Por qué separado de GameUiState: si el "Snap" o las partículas vivieran en el
 * StateFlow persistente, una recomposición por cualquier otro motivo los
 * volvería a disparar. Estos eventos se consumen UNA vez vía SharedFlow.
 */
import androidx.compose.runtime.Immutable
import com.hoshiraflow.domain.model.Cell
import com.hoshiraflow.domain.model.PuzzleColor

@Immutable
sealed class GameEvent {
    data class NodeSnapped(val color: PuzzleColor, val cell: Cell) : GameEvent()
    data class ColorCompleted(val color: PuzzleColor, val atCell: Cell) : GameEvent()
    object LevelCompleted : GameEvent()
    object InvalidMove : GameEvent() // para un haptic sutil de "rechazo"
}



