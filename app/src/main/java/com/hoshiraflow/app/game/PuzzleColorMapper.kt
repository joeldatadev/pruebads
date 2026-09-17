package com.hoshiraflow.app.game

/**
 * Ruta destino: app/src/main/java/com/zenflow/app/game/PuzzleColorMapper.kt
 */
import androidx.compose.ui.graphics.Color
import com.hoshiraflow.domain.model.PuzzleColor

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
    PuzzleColor.COLOR_10 -> Color(0xFF4EE2C0)
    PuzzleColor.COLOR_11 -> Color(0xFFFF6B6B)
    PuzzleColor.COLOR_12 -> Color(0xFF4D96FF)
    PuzzleColor.COLOR_13 -> Color(0xFF6BCB77)
    PuzzleColor.COLOR_14 -> Color(0xFFFFD93D)
    PuzzleColor.COLOR_15 -> Color(0xFF9B5DE5)
    PuzzleColor.COLOR_16 -> Color(0xFFF15BB5)
    PuzzleColor.COLOR_17 -> Color(0xFF00BBF9)
    PuzzleColor.COLOR_18 -> Color(0xFF00F5D4)
    PuzzleColor.COLOR_19 -> Color(0xFFEEEF20)
    PuzzleColor.COLOR_20 -> Color(0xFFFF9F1C)
}



