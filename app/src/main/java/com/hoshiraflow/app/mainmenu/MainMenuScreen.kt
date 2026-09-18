package com.hoshiraflow.app.mainmenu

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.hoshiraflow.app.levelselect.LevelSelectViewModel
import com.hoshiraflow.app.levelselect.LevelSelectViewModelFactory
import com.hoshiraflow.domain.repository.DailyChallengeRepository
import com.hoshiraflow.domain.repository.LevelRepository
import com.hoshiraflow.domain.repository.ProgressRepository

private val BackgroundGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF0B0E14), Color(0xFF161C24))
)

private val CTA_Gradient = Brush.linearGradient(
    colors = listOf(Color(0xFF06B6D4), Color(0xFF8B5CF6))
)

private val DailyGradient = Brush.linearGradient(
    colors = listOf(Color(0xFFFF7A45), Color(0xFFE23E57))
)

private val InfiniteGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF0FB5AE), Color(0xFF146C94))
)

private val PortalGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF8B5CF6), Color(0xFF06B6D4))
)

private val SwitchGradient = Brush.linearGradient(
    colors = listOf(Color(0xFFF59E0B), Color(0xFFEC4899))
)

private val MasterGradient = Brush.linearGradient(
    colors = listOf(Color(0xFFFFD700), Color(0xFF990000))
)



@Composable
fun MainMenuScreen(
    levelRepository: LevelRepository,
    progressRepository: ProgressRepository,
    dailyChallengeRepository: DailyChallengeRepository,
    onCampaignSelected: () -> Unit,
    onDailyChallengeSelected: () -> Unit,
    onSettingsSelected: () -> Unit,
    onPortalModeSelected: (com.hoshiraflow.domain.model.BoardShape?) -> Unit = {},
    onSwitchModeSelected: (com.hoshiraflow.domain.model.BoardShape?) -> Unit = {},
    onMasterModeSelected: (com.hoshiraflow.domain.model.BoardShape?) -> Unit = {},
    onInfiniteModeSelected: (com.hoshiraflow.domain.model.BoardShape?) -> Unit = {},
    onCubeModeSelected: () -> Unit = {},
    onEmptyCubeSelected: () -> Unit = {}
) {
    val viewModel: LevelSelectViewModel = viewModel(
        factory = LevelSelectViewModelFactory(levelRepository, progressRepository, dailyChallengeRepository)
    )
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGradient)
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(50.dp))

        Text(
            text = "Lumina Lines",
            color = Color.White,
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )

        Text(
            text = "Encuentra tu centro",
            color = Color(0xFF94A3B8),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(24.dp))

        var selectedShape by remember { mutableStateOf<com.hoshiraflow.domain.model.BoardShape?>(null) }

        // Ultra-compact modern Segmented glassmorphism pills row selector
        androidx.compose.foundation.lazy.LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF1E293B).copy(alpha = 0.4f))
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val gridSizes = listOf(
                "Predet." to null,
                "5x5" to com.hoshiraflow.domain.model.BoardShape.SQUARE(5),
                "6x6" to com.hoshiraflow.domain.model.BoardShape.SQUARE(6),
                "7x7" to com.hoshiraflow.domain.model.BoardShape.SQUARE(7),
                "8x8" to com.hoshiraflow.domain.model.BoardShape.SQUARE(8),
                "9x9" to com.hoshiraflow.domain.model.BoardShape.SQUARE(9),
                "10x10" to com.hoshiraflow.domain.model.BoardShape.SQUARE(10),
                "11x11" to com.hoshiraflow.domain.model.BoardShape.SQUARE(11),
                "12x12" to com.hoshiraflow.domain.model.BoardShape.SQUARE(12),
                "13x13" to com.hoshiraflow.domain.model.BoardShape.SQUARE(13),
                "11x14" to com.hoshiraflow.domain.model.BoardShape.RECTANGLE(11, 14),
                "⏳ Reloj de Arena" to com.hoshiraflow.domain.model.BoardShape.HOURGLASS(11, 11)
            )
            items(gridSizes.size) { index ->
                val (label, shape) = gridSizes[index]
                val isSelected = selectedShape == shape
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) Color(0xFF8B5CF6) else Color.Transparent)
                        .clickable { selectedShape = shape }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Card 1: Campaña (Frosted Cyan/Purple gradient with progress bar)
        PremiumMenuCard(
            title = "Campaña",
            subtitle = "${uiState.completedLevels.size} / ${uiState.totalLevels} Niveles completed",
            icon = Icons.Filled.PlayArrow,
            gradient = CTA_Gradient,
            onClick = onCampaignSelected,
            progress = if (uiState.totalLevels > 0) uiState.completedLevels.size.toFloat() / uiState.totalLevels else 0f
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Grid Row 1: Próximo Reto (Daily) | Infinito
        Row(modifier = Modifier.fillMaxWidth()) {
            SmallMenuCard(
                title = if (uiState.isDailyChallengeCompletedToday) "Próximo Reto" else "Reto Diario",
                modifier = Modifier.weight(1f),
                subtitle = if (uiState.isDailyChallengeCompletedToday) uiState.dailyChallengeCountdown else "13:06:41",
                icon = Icons.Filled.LocalFireDepartment,
                gradient = DailyGradient,
                onClick = onDailyChallengeSelected
            )
            
            Spacer(modifier = Modifier.width(16.dp))

            SmallMenuCard(
                title = "Infinito",
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.AllInclusive,
                gradient = InfiniteGradient,
                onClick = { onInfiniteModeSelected(selectedShape) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Grid Row 2: Modo Portales | Modo Interruptores
        Row(modifier = Modifier.fillMaxWidth()) {
            SmallMenuCard(
                title = "Modo Portales",
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.VpnKey,
                gradient = PortalGradient,
                onClick = { onPortalModeSelected(selectedShape) }
            )
            
            Spacer(modifier = Modifier.width(16.dp))

            SmallMenuCard(
                title = "Modo Interruptores",
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Tune,
                gradient = SwitchGradient,
                onClick = { onSwitchModeSelected(selectedShape) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Card 6: Modo Maestro / Caos (Full-width premium Gold/Crimson card)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MasterGradient)
                .clickable { onMasterModeSelected(selectedShape) }
                .padding(20.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(text = "Modo Maestro (Caos)", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Combinación extrema de mecánicas", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                    }
                }
                Icon(Icons.Filled.MilitaryTech, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Card 7: Desafío del Cubo 3D (New Mode)
        PremiumMenuCard(
            title = "Desafío del Cubo 3D",
            subtitle = "${uiState.completedCubeLevels.size} / ${uiState.totalCubeLevels} Niveles superados",
            icon = Icons.Filled.ViewInAr,
            gradient = Brush.linearGradient(colors = listOf(Color(0xFF10B981), Color(0xFF06B6D4))),
            onClick = onCubeModeSelected,
            progress = if (uiState.totalCubeLevels > 0) uiState.completedCubeLevels.size.toFloat() / uiState.totalCubeLevels else 0f
        )



        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "🧪 Herramientas de Diagnóstico",
            color = Color(0xFFFACC15),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start).padding(bottom = 12.dp)
        )

        SmallMenuCard(
            title = "Pirámide 3D (Debug)",
            modifier = Modifier.fillMaxWidth(),
            subtitle = "Validación de estructura y capas",
            icon = Icons.Filled.Layers,
            gradient = Brush.linearGradient(colors = listOf(Color(0xFF6366F1), Color(0xFF4338CA))),
            onClick = onEmptyCubeSelected
        )

        Spacer(modifier = Modifier.height(30.dp))

        IconButton(
            onClick = onSettingsSelected,
            modifier = Modifier
                .padding(bottom = 32.dp)
                .size(56.dp)
                .clip(CircleShape)
                .background(Color(0xFF334155).copy(alpha = 0.5f))
        ) {
            Icon(Icons.Filled.Settings, contentDescription = "Ajustes", tint = Color.White)
        }
    }
}

@Composable
private fun PremiumMenuCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradient: Brush,
    onClick: () -> Unit,
    progress: Float
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(gradient)
            .clickable(onClick = onClick)
            .padding(20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column(verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
                
                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(text = title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(text = subtitle, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(14.dp))
            
            LinearProgressIndicator(
                progress = { progress },
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.3f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
            )
        }
    }
}

@Composable
private fun SmallMenuCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector,
    gradient: Brush,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(145.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(gradient)
            .clickable(onClick = onClick)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 14.sp
                )
            }
        }
    }
}
