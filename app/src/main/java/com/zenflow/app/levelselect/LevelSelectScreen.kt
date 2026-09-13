package com.zenflow.app.levelselect

/**
 * Ruta destino: app/src/main/java/com/zenflow/app/levelselect/LevelSelectScreen.kt (REEMPLAZA el archivo completo)
 * Cambio: menú principal rediseñado con jerarquía visual clara:
 *   1. Reto Diario (hero, full-width, máxima prioridad -> racha = mayor LTV)
 *   2. Campaña (card con anillo de progreso, abre la grilla de niveles al tocar)
 *   3. Infinito (card con glow cian, acceso directo sin fricción)
 * La grilla de 100 niveles ya NO se muestra siempre: solo al entrar a "Campaña",
 * para no abrumar al jugador con 100 casillas apenas abre la app.
 */
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zenflow.domain.model.DailyChallengeState
import com.zenflow.domain.repository.DailyChallengeRepository
import com.zenflow.domain.repository.LevelRepository
import com.zenflow.domain.repository.ProgressRepository
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    var showCampaignGrid by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = !showCampaignGrid,
            enter = fadeIn(tween(200)) + slideInHorizontally(tween(250)) { -it / 4 },
            exit = fadeOut(tween(150)) + slideOutHorizontally(tween(200)) { -it / 4 }
        ) {
            MainMenu(
                uiState = uiState,
                onDailyChallengeSelected = onDailyChallengeSelected,
                onCampaignSelected = { showCampaignGrid = true },
                onInfiniteModeSelected = onInfiniteModeSelected,
                onSettingsSelected = onSettingsSelected
            )
        }

        AnimatedVisibility(
            visible = showCampaignGrid,
            enter = fadeIn(tween(200)) + slideInHorizontally(tween(250)) { it / 4 },
            exit = fadeOut(tween(150)) + slideOutHorizontally(tween(200)) { it / 4 }
        ) {
            CampaignGrid(
                uiState = uiState,
                onBack = { showCampaignGrid = false },
                onLevelSelected = onLevelSelected
            )
        }
    }
}

@Composable
private fun MainMenu(
    uiState: LevelSelectUiState,
    onDailyChallengeSelected: () -> Unit,
    onCampaignSelected: () -> Unit,
    onInfiniteModeSelected: () -> Unit,
    onSettingsSelected: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Zen Flow",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = androidx.compose.ui.unit.TextUnit(26f, androidx.compose.ui.unit.TextUnitType.Sp)
            )
            IconButton(onClick = onSettingsSelected) {
                Icon(Icons.Filled.Settings, contentDescription = "Ajustes", tint = Color(0xFF9CA3AF))
            }
        }

        Spacer(Modifier.height(20.dp))

        // --- HERO: Reto Diario (máxima prioridad visual) ---
        DailyChallengeCard(
            state = uiState.dailyChallengeState,
            onClick = onDailyChallengeSelected
        )

        Spacer(Modifier.height(12.dp))

        // --- Fila secundaria: Campaña + Infinito, mismo peso visual ---
        Row(modifier = Modifier.fillMaxWidth()) {
            CampaignCard(
                completedCount = uiState.completedLevels.size,
                totalLevels = uiState.totalLevels,
                onClick = onCampaignSelected,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(12.dp))
            InfiniteCard(
                onClick = onInfiniteModeSelected,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.weight(1f))

        Text(
            text = "Hecho para respirar, un tablero a la vez 🧘",
            color = Color(0xFF4B5563),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun CampaignGrid(
    uiState: LevelSelectUiState,
    onBack: () -> Unit,
    onLevelSelected: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
            }
            Spacer(Modifier.width(4.dp))
            Text("Campaña", color = Color.White, fontWeight = FontWeight.Bold)
        }

        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            when {
                uiState.isLoading -> CircularProgressIndicator()
                uiState.errorMessage != null -> Text("Error: ${uiState.errorMessage}", color = Color.White)
                uiState.totalLevels == 0 -> Text("No hay niveles todavía", color = Color.White)
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)
                ) {
                    items(uiState.totalLevels) { index ->
                        val levelNumber = index + 1
                        LevelCell(
                            levelNumber = levelNumber,
                            isCompleted = levelNumber in uiState.completedLevels,
                            onClick = { onLevelSelected(levelNumber) }
                        )
                    }
                }
            }
        }
    }
}

// ========================= HERO CARD: RETO DIARIO =========================

private val STREAK_MILESTONES = listOf(7, 30, 100, 365)

private fun flameColorFor(streak: Int): Color = when {
    streak <= 0 -> Color(0xFF6B7280)
    streak < 30 -> Color(0xFFFFD700)
    streak < 100 -> Color(0xFF00E5FF)
    else -> Color(0xFF9D00FF)
}

@Composable
private fun DailyChallengeCard(
    state: DailyChallengeState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val todayEpochDay = remember { LocalDate.now(ZoneOffset.UTC).toEpochDay() }
    val completedToday = state.isCompletedOn(todayEpochDay)

    val flameScale = remember { Animatable(1f) }
    var lastSeenStreak by remember { mutableIntStateOf(state.currentStreak) }
    var celebrationText by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.currentStreak) {
        val crossed = STREAK_MILESTONES.firstOrNull { lastSeenStreak < it && state.currentStreak >= it }
        if (crossed != null) {
            celebrationText = "¡$crossed días seguidos! 🔥"
            scope.launch {
                flameScale.animateTo(1.6f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
                flameScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            }
            scope.launch { delay(2200); celebrationText = null }
        }
        lastSeenStreak = state.currentStreak
    }

    val infiniteTransition = rememberInfiniteTransition(label = "flame_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "flame_glow_alpha"
    )
    val isPulsingTier = state.currentStreak in 7..29

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(listOf(Color(0xFF2A1F3D), Color(0xFF1E2230))),
                    shape = RoundedCornerShape(20.dp)
                )
                .clickable(enabled = !completedToday, onClick = onClick)
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = Icons.Filled.LocalFireDepartment,
                    contentDescription = null,
                    modifier = Modifier
                        .size(36.dp)
                        .scale(flameScale.value)
                        .graphicsLayer { shadowElevation = if (flameScale.value > 1.05f) 12f else 0f },
                    tint = flameColorFor(state.currentStreak).let { c -> if (isPulsingTier) c.copy(alpha = glowAlpha) else c }
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (completedToday) "Reto Diario completado ✓" else "Reto Diario",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = androidx.compose.ui.unit.TextUnit(18f, androidx.compose.ui.unit.TextUnitType.Sp)
                    )
                    Text(
                        text = "Racha: ${state.currentStreak} día${if (state.currentStreak == 1) "" else "s"}" +
                                if (state.bestStreak > state.currentStreak) " · Mejor: ${state.bestStreak}" else "",
                        color = flameColorFor(state.currentStreak)
                    )
                }
                if (!completedToday) {
                    Text("Jugar →", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                }
            }
        }

        AnimatedVisibility(visible = celebrationText != null, enter = fadeIn(tween(200)), exit = fadeOut(tween(400))) {
            Text(
                text = celebrationText.orEmpty(),
                color = flameColorFor(state.currentStreak),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 6.dp, start = 4.dp)
            )
        }
    }
}

// ========================= CARD: CAMPAÑA (con anillo de progreso) =========================

@Composable
private fun CampaignCard(
    completedCount: Int,
    totalLevels: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (totalLevels > 0) completedCount.toFloat() / totalLevels else 0f

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(Color(0xFF1E2230), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f)) {
                Canvas(modifier = Modifier.size(64.dp)) {
                    val stroke = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                    drawArc(
                        color = Color(0xFF2A2A2A),
                        startAngle = -90f, sweepAngle = 360f, useCenter = false,
                        style = stroke
                    )
                    drawArc(
                        color = Color(0xFF43A047),
                        startAngle = -90f, sweepAngle = 360f * progress, useCenter = false,
                        style = stroke
                    )
                }
                Text("$completedCount/$totalLevels", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Text("Campaña", color = Color.White, fontWeight = FontWeight.Bold)
            Text("Niveles diseñados", color = Color(0xFF9CA3AF))
        }
    }
}

// ========================= CARD: INFINITO (glow cian) =========================

@Composable
private fun InfiniteCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "infinite_glow")
    val glow by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "infinite_glow_alpha"
    )

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(
                brush = Brush.radialGradient(listOf(Color(0xFF0E3A42), Color(0xFF1E2230))),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = Icons.Filled.AllInclusive,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF).copy(alpha = glow),
                    modifier = Modifier.size(40.dp)
                )
            }
            Text("Infinito", color = Color.White, fontWeight = FontWeight.Bold)
            Text("Sin límite", color = Color(0xFF9CA3AF))
        }
    }
}

@Composable
private fun LevelCell(
    levelNumber: Int,
    isCompleted: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isCompleted) Color(0xFF43A047) else Color(0xFF2A2A2A)

    Box(
        modifier = Modifier
            .padding(8.dp)
            .aspectRatio(1f)
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = levelNumber.toString(), color = Color.White, fontWeight = FontWeight.Bold)
    }
}