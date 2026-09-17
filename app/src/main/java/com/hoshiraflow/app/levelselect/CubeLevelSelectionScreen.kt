package com.hoshiraflow.app.levelselect

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hoshiraflow.domain.repository.DailyChallengeRepository
import com.hoshiraflow.domain.repository.LevelRepository
import com.hoshiraflow.domain.repository.ProgressRepository

private val BackgroundGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B))
)

private val CubeProgressGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF10B981), Color(0xFF06B6D4))
)

@Composable
fun CubeLevelSelectionScreen(
    levelRepository: LevelRepository,
    progressRepository: ProgressRepository,
    dailyChallengeRepository: DailyChallengeRepository,
    onLevelSelected: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: LevelSelectViewModel = viewModel(
        factory = LevelSelectViewModelFactory(levelRepository, progressRepository, dailyChallengeRepository)
    )
    val uiState by viewModel.uiState.collectAsState()
    
    val totalCubeLevels = uiState.totalCubeLevels
    val completedLevels = uiState.completedCubeLevels

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundGradient)
    ) {
        // Header
        CubeHeader(
            completedCount = completedLevels.size,
            totalCount = totalCubeLevels,
            onBack = onBack
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "🧊 Desafío del Cubo 3D",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(
                count = totalCubeLevels,
                key = { it + 1 }
            ) { index ->
                val levelNumber = index + 1
                val isCompleted = levelNumber in completedLevels
                val isLocked = levelNumber > completedLevels.size + 1
                val isCurrent = levelNumber == completedLevels.size + 1

                CubeLevelCard(
                    levelNumber = levelNumber,
                    isCompleted = isCompleted,
                    isLocked = isLocked,
                    isCurrent = isCurrent,
                    onClick = { if (!isLocked) onLevelSelected(levelNumber) }
                )
            }
        }
    }
}

@Composable
private fun CubeHeader(
    completedCount: Int,
    totalCount: Int,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFF334155).copy(alpha = 0.5f))
        ) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Maestría del Cubo",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(Color(0xFF334155))
        ) {
            val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceAtLeast(0.01f))
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(CubeProgressGradient)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Explorando las 3 caras del pensamiento",
            color = Color(0xFF10B981),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun CubeLevelCard(
    levelNumber: Int,
    isCompleted: Boolean,
    isLocked: Boolean,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isLocked) Color(0xFF334155).copy(alpha = 0.4f)
                else Color(0xFF1E293B)
            )
            .border(
                width = 2.dp,
                color = when {
                    isCompleted -> Color(0xFF10B981)
                    isCurrent -> Color(0xFF8B5CF6)
                    else -> Color.White.copy(alpha = 0.1f)
                },
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(enabled = !isLocked, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isLocked) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = "Bloqueado",
                tint = Color(0xFF64748B),
                modifier = Modifier.size(20.dp)
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = levelNumber.toString(),
                    color = if (isCurrent) Color(0xFF8B5CF6) else Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}
