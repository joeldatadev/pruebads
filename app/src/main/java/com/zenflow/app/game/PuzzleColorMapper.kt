package com.zenflow.app.game

/**
 * Ruta destino: app/src/main/java/com/zenflow/app/game/PuzzleColorMapper.kt
 */
import androidx.compose.ui.graphics.Color
import com.zenflow.domain.model.PuzzleColor

fun PuzzleColor.toComposeColor(): Color = when (this) {
    PuzzleColor.RED -> Color(0xFFE53935)
    PuzzleColor.BLUE -> Color(0xFF1E88E5)
    PuzzleColor.GREEN -> Color(0xFF43A047)
    PuzzleColor.YELLOW -> Color(0xFFFDD835)
    PuzzleColor.ORANGE -> Color(0xFFFB8C00)
    PuzzleColor.PURPLE -> Color(0xFF8E24AA)
    PuzzleColor.CYAN -> Color(0xFF00ACC1)
    PuzzleColor.PINK -> Color(0xFFD81B60)
    PuzzleColor.BROWN -> Color(0xFF6D4C41)
    PuzzleColor.GRAY -> Color(0xFF757575)
}
