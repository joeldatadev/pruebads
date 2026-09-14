package com.zenflow.app.mainmenu

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.zenflow.app.levelselect.LevelSelectViewModel
import com.zenflow.app.levelselect.LevelSelectViewModelFactory
import com.zenflow.domain.repository.DailyChallengeRepository
import com.zenflow.domain.repository.LevelRepository
import com.zenflow.domain.repository.ProgressRepository

private val BackgroundGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B))
)

private val CTA_Gradient = Brush.linearGradient(
    colors = listOf(Color(0xFF8B5CF6), Color(0xFF06B6D4))
)

private val DailyGradient = Brush.linearGradient(
    colors = listOf(Color(0xFFFF7A45), Color(0xFFE23E57))
)

private val InfiniteGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF0FB5AE), Color(0xFF146C94))
)

@Composable
fun MainMenuScreen(
    levelRepository: LevelRepository,
    progressRepository: ProgressRepository,
    dailyChallengeRepository: DailyChallengeRepository,
    onCampaignSelected: () -> Unit,
    onDailyChallengeSelected: () -> Unit,
    onInfiniteModeSelected: () -> Unit,
    onSettingsSelected: () -> Unit
) {
    val viewModel: LevelSelectViewModel = viewModel(
        factory = LevelSelectViewModelFactory(levelRepository, progressRepository, dailyChallengeRepository)
    )
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGradient)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        Text(
            text = "Zen Flow",
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

        Spacer(modifier = Modifier.weight(1f))

        MenuCard(
            title = "Campaña",
            subtitle = "${uiState.completedLevels.size} / ${uiState.totalLevels} Niveles",
            icon = Icons.Filled.PlayArrow,
            gradient = CTA_Gradient,
            onClick = onCampaignSelected
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            SmallMenuCard(
                title = "Reto Diario",
                icon = Icons.Filled.LocalFireDepartment,
                gradient = DailyGradient,
                modifier = Modifier.weight(1f),
                onClick = onDailyChallengeSelected
            )
            
            Spacer(modifier = Modifier.width(16.dp))

            SmallMenuCard(
                title = "Infinito",
                icon = Icons.Filled.AllInclusive,
                gradient = InfiniteGradient,
                modifier = Modifier.weight(1f),
                onClick = onInfiniteModeSelected
            )
        }

        Spacer(modifier = Modifier.weight(1f))

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
private fun MenuCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradient: Brush,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(gradient)
            .clickable(onClick = onClick)
            .padding(24.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
            
            Spacer(modifier = Modifier.width(20.dp))

            Column {
                Text(text = title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(text = subtitle, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun SmallMenuCard(
    title: String,
    icon: ImageVector,
    gradient: Brush,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(140.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(gradient)
            .clickable(onClick = onClick)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
