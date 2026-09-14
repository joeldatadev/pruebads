package com.zenflow.app.levelselect

/**
 * Ruta destino: app/src/main/java/com/zenflow/app/levelselect/LevelSelectScreen.kt (REEMPLAZA el archivo completo)
 *
 * Rediseño: 3 botones/tarjetas de peso visual similar (Campaña / Reto del día /
 * Infinito) en vez de un héroe gigante + los 100 niveles siempre visibles.
 * Campaña ahora ABRE la grilla de 100 niveles como una vista separada (con
 * botón de volver) en lugar de mostrarla siempre en la pantalla principal.
 *
 * EFICIENCIA (a propósito, no por descuido):
 * - Cero animaciones infinitas/en bucle en este archivo (nada de
 *   rememberInfiniteTransition ni glow pulsante) -- eso obliga a recomponer
 *   y redibujar constantemente aunque el usuario no toque nada, lo cual
 *   calienta el dispositivo y gasta batería sin necesidad. Todo acá es
 *   estático hasta que el usuario interactúa; la única animación es la
 *   transición (una sola vez, ~250ms) entre el menú y la grilla de niveles.
 * - Brushes y colores están definidos como constantes top-level (no se
 *   recrean en cada recomposición).
 */
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
private val DailyGradient = Brush.linearGradient(colors = listOf(Color(0xFFFF7A45), Color(0xFFE23E57)))
private val CampaignGradient = Brush.linearGradient(colors = listOf(Color(0xFF7C5CFF), Color(0xFF4B3AA8)))
private val InfiniteGradient = Brush.linearGradient(colors = listOf(Color(0xFF0FB5AE), Color(0xFF146C94)))

private val CardHeight = 92.dp

@Composable
fun LevelSelectScreen(
    levelRepository: LevelRepository,
    progressRepository: ProgressRepository,
    dailyChallengeRepository: DailyChallengeRepository,
    onLevelSelected: (Int) -> Unit,
    onInfiniteModeSelected: () -> Unit = {},
    onDailyChallengeSelected: () -> Unit = {},
    onSettingsSelected: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val viewModel: LevelSelectViewModel = viewModel(
        factory = LevelSelectViewModelFactory(levelRepository, progressRepository, dailyChallengeRepository)
    )
    val uiState by viewModel.uiState.collectAsState()
    var showCampaignGrid by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundGradient)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (showCampaignGrid) "Campaña" else "Zen Flow",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (!showCampaignGrid) {
                IconButton(onClick = onSettingsSelected) {
                    Icon(Icons.Filled.Settings, contentDescription = "Ajustes", tint = Color(0xFFB8BCC8))
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // AnimatedContent, no un remember de estado a lo largo de toda la app:
        // la transición corre UNA vez al cambiar de vista y termina; no queda
        // ningún recompositor activo de fondo mientras el usuario mira el menú
        // o la grilla quieto.
        AnimatedContent(
            targetState = showCampaignGrid,
            transitionSpec = {
                if (targetState) {
                    (slideInHorizontally(initialOffsetX = { it / 3 }) + fadeIn()) togetherWith
                        (slideOutHorizontally(targetOffsetX = { -it / 3 }) + fadeOut())
                } else {
                    (slideInHorizontally(initialOffsetX = { -it / 3 }) + fadeIn()) togetherWith
                        (slideOutHorizontally(targetOffsetX = { it / 3 }) + fadeOut())
                }
            },
            modifier = Modifier.weight(1f),
            label = "level_select_content"
        ) { onCampaignGrid ->
            if (onCampaignGrid) {
                CampaignGridContent(
                    uiState = uiState,
                    onBack = { showCampaignGrid = false },
                    onLevelSelected = onLevelSelected
                )
            } else {
                MenuHome(
                    uiState = uiState,
                    onCampaignSelected = { showCampaignGrid = true },
                    onDailyChallengeSelected = onDailyChallengeSelected,
                    onInfiniteModeSelected = onInfiniteModeSelected
                )
            }
        }
    }
}

@Composable
private fun MenuHome(
    uiState: LevelSelectUiState,
    onCampaignSelected: () -> Unit,
    onDailyChallengeSelected: () -> Unit,
    onInfiniteModeSelected: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        MenuActionCard(
            title = "Campaña",
            subtitle = "100 niveles diseñados",
            icon = Icons.Filled.SportsEsports,
            gradient = CampaignGradient,
            trailingBadge = { CountBadge(count = uiState.completedLevels.size, total = uiState.totalLevels) },
            onClick = onCampaignSelected
        )

        Spacer(modifier = Modifier.height(14.dp))

        MenuActionCard(
            title = if (uiState.isDailyChallengeCompletedToday) "Reto de hoy completado" else "Reto del día",
            subtitle = if (uiState.dailyChallengeState.currentStreak > 0) {
                "Racha: ${uiState.dailyChallengeState.currentStreak} ${if (uiState.dailyChallengeState.currentStreak == 1) "día" else "días"}"
            } else {
                "Empezá tu racha hoy"
            },
            icon = Icons.Filled.LocalFireDepartment,
            gradient = DailyGradient,
            trailingBadge = {
                if (uiState.isDailyChallengeCompletedToday) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = "Completado", tint = Color.White, modifier = Modifier.size(26.dp))
                } else if (uiState.dailyChallengeState.currentStreak > 0) {
                    StreakBadge(uiState.dailyChallengeState.currentStreak)
                }
            },
            onClick = onDailyChallengeSelected
        )

        Spacer(modifier = Modifier.height(14.dp))

        MenuActionCard(
            title = "Infinito",
            subtitle = "Tableros sin límite",
            icon = Icons.Filled.AllInclusive,
            gradient = InfiniteGradient,
            trailingBadge = null,
            onClick = onInfiniteModeSelected
        )
    }
}

/** Tarjeta base reusada por las 3 opciones -- mismo alto, mismo estilo, para que se vean como "3 botones" parejos. */
@Composable
private fun MenuActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradient: Brush,
    trailingBadge: (@Composable () -> Unit)?,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(CardHeight)
            .clip(RoundedCornerShape(20.dp))
            .background(gradient)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text(text = subtitle, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
            }

            if (trailingBadge != null) {
                Spacer(modifier = Modifier.width(8.dp))
                trailingBadge()
            }
        }
    }
}

/** Badge "X/100" en formato píldora en vez de texto plano -- mismo lenguaje visual que el contador del tablero. */
@Composable
private fun CountBadge(count: Int, total: Int) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.18f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text = "$count/$total", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StreakBadge(streak: Int) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.18f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text = "🔥 $streak", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CampaignGridContent(
    uiState: LevelSelectUiState,
    onBack: () -> Unit,
    onLevelSelected: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(4.dp))
            CountBadge(count = uiState.completedLevels.size, total = uiState.totalLevels)
        }

        Box(modifier = Modifier.weight(1f)) {
            when {
                uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
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
