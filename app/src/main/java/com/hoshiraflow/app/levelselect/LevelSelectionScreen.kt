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

private val ProgressGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF8B5CF6), Color(0xFF06B6D4))
)

@Composable
fun LevelSelectionScreen(
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundGradient)
    ) {
        // Header
        Header(
            completedCount = uiState.completedLevels.size,
            totalCount = uiState.totalLevels,
            totalStars = uiState.completedLevels.size * 3, // Assuming 3 stars for simplicity for now
            onBack = onBack
        )

        if (uiState.isLoading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF8B5CF6))
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = "🗺️ Niveles de Campaña",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                items(
                    count = uiState.totalLevels,
                    key = { it + 1 }
                ) { index ->
                    val levelNumber = index + 1
                    val isCompleted = levelNumber in uiState.completedLevels
                    val isLocked = levelNumber > uiState.completedLevels.size + 1
                    val isCurrent = levelNumber == uiState.completedLevels.size + 1

                    LevelCard(
                        levelNumber = levelNumber,
                        isCompleted = isCompleted,
                        isLocked = isLocked,
                        isCurrent = isCurrent,
                        stars = if (isCompleted) 3 else 0,
                        onClick = { if (!isLocked) onLevelSelected(levelNumber) }
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(
    completedCount: Int,
    totalCount: Int,
    totalStars: Int,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
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

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF334155).copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = totalStars.toString(), color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Progreso",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(CircleShape)
                .background(Color(0xFF334155))
        ) {
            val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(ProgressGradient)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "$completedCount / $totalCount Niveles",
            color = Color(0xFF94A3B8),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun LevelCard(
    levelNumber: Int,
    isCompleted: Boolean,
    isLocked: Boolean,
    isCurrent: Boolean,
    stars: Int,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isCurrent) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .scale(if (isCurrent) scale else 1f)
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (isLocked) Color(0xFF334155).copy(alpha = 0.4f)
                else Color(0xFF1E293B)
            )
            .then(
                if (isCompleted) Modifier.border(2.dp, Color(0xFF10B981), RoundedCornerShape(18.dp))
                else if (isCurrent) Modifier.border(2.dp, Color(0xFF8B5CF6), RoundedCornerShape(18.dp))
                else Modifier
            )
            .clickable(enabled = !isLocked, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isLocked) {
            Icon(Icons.Filled.Lock, contentDescription = "Bloqueado", tint = Color(0xFF64748B), modifier = Modifier.size(24.dp))
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = levelNumber.toString(),
                    color = if (isCurrent) Color(0xFF8B5CF6) else Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                if (isCompleted) {
                    Row {
                        repeat(stars) {
                            Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }
        }
    }
}



