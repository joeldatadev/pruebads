package com.zenflow.app.levelselect

/**
 * Ruta destino: app/src/main/java/com/zenflow/app/levelselect/LevelSelectScreen.kt (REEMPLAZA el archivo)
 *
 * Decisiones de diseño (no arbitrarias):
 * - El Reto Diario es el héroe (gradiente ámbar/rojo = fuego = la racha, no un
 *   gradiente decorativo). Es lo primero que se ve, ocupa todo el ancho.
 * - Modo Infinito usa un gradiente teal/azul (evoca flujo continuo/agua) para
 *   diferenciarse visualmente del calor del reto diario.
 * - Las celdas de nivel usan los MISMOS colores del puzzle (PuzzleColorMapper)
 *   como acento, cicladados por número - conecta visualmente la selección de
 *   niveles con el propio juego, en vez de una grilla gris sin relación.
 * - Grid adaptativo (GridCells.Adaptive) en vez de columnas fijas, para que
 *   se vea bien en cualquier ancho de pantalla.
 */
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zenflow.app.game.toComposeColor
import com.zenflow.domain.model.PuzzleColor
import com.zenflow.domain.repository.DailyChallengeRepository
import com.zenflow.domain.repository.LevelRepository
import com.zenflow.domain.repository.ProgressRepository

private val BackgroundGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF0B0D14), Color(0xFF15111F))
)
private val DailyGradient = Brush.linearGradient(
    colors = listOf(Color(0xFFFF7A45), Color(0xFFE23E57))
)
private val InfiniteGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF0FB5AE), Color(0xFF146C94))
)

@Composable
fun LevelSelectScreen(
    levelRepository: LevelRepository,
    progressRepository: ProgressRepository,
    dailyChallengeRepository: DailyChallengeRepository,
    onLevelSelected: (Int) -> Unit,
    onInfiniteModeSelected: () -> Unit = {},
    onDailyChallengeSelected: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val viewModel: LevelSelectViewModel = viewModel(
        factory = LevelSelectViewModelFactory(levelRepository, progressRepository, dailyChallengeRepository)
    )
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundGradient)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Zen Flow",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(20.dp))

        DailyChallengeHero(
            completedToday = uiState.isDailyChallengeCompletedToday,
            streak = uiState.dailyChallengeState.currentStreak,
            onClick = onDailyChallengeSelected
        )

        Spacer(modifier = Modifier.height(12.dp))

        InfiniteModeCard(onClick = onInfiniteModeSelected)

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Niveles",
            color = Color(0xFFB8BCC8),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(12.dp))

        Box(modifier = Modifier.weight(1f)) {
            when {
                uiState.isLoading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
                uiState.errorMessage != null -> Text(
                    "Error: ${uiState.errorMessage}",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
                uiState.totalLevels == 0 -> Text(
                    "No hay niveles todavía",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 76.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.totalLevels) { index ->
                        val levelNumber = index + 1
                        val accentColor = PuzzleColor.entries[index % PuzzleColor.entries.size].toComposeColor()
                        LevelCell(
                            levelNumber = levelNumber,
                            isCompleted = levelNumber in uiState.completedLevels,
                            accentColor = accentColor,
                            onClick = { onLevelSelected(levelNumber) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyChallengeHero(
    completedToday: Boolean,
    streak: Int,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(DailyGradient)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.LocalFireDepartment,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = if (completedToday) "Reto de hoy completado" else "Reto del día",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (streak > 0) "Racha: $streak ${if (streak == 1) "día" else "días"}" else "Empieza tu racha",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp
                )
            }
        }

        if (completedToday) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Completado",
                tint = Color.White,
                modifier = Modifier.align(Alignment.CenterEnd).size(26.dp)
            )
        }
    }
}

@Composable
private fun InfiniteModeCard(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(InfiniteGradient)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = "∞  Modo Infinito",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun LevelCell(
    levelNumber: Int,
    isCompleted: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1A1C24))
            .border(
                width = if (isCompleted) 2.dp else 1.dp,
                color = if (isCompleted) accentColor else accentColor.copy(alpha = 0.35f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = levelNumber.toString(),
            color = if (isCompleted) accentColor else Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp
        )

        if (isCompleted) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Completado",
                    tint = Color(0xFF1A1C24),
                    modifier = Modifier.size(10.dp)
                )
            }
        }
    }
}