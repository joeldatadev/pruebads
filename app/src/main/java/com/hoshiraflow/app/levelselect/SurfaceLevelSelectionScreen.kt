package com.hoshiraflow.app.levelselect

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.foundation.lazy.grid.items
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hoshiraflow.domain.repository.DailyChallengeRepository
import com.hoshiraflow.domain.repository.LevelRepository
import com.hoshiraflow.domain.repository.ProgressRepository

private val BackgroundGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B))
)

private val SurfaceProgressGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
)

data class SurfaceShapeItem(
    val name: String,
    val icon: String,
    val description: String
)

val SurfaceShapesList = listOf(
    SurfaceShapeItem("Escalera", "🪜", "Rampa diagonal en desnivel continuo"),
    SurfaceShapeItem("Pirámide", "🏔️", "Pico en una esquina, base escalonada"),
    SurfaceShapeItem("Zigurat", "🛕", "Terrazas anchas tipo templo"),
    SurfaceShapeItem("Rampa", "🛹", "Pasillo largo en descenso")
)

@Composable
fun SurfaceLevelSelectionScreen(
    levelRepository: LevelRepository,
    progressRepository: ProgressRepository,
    dailyChallengeRepository: DailyChallengeRepository,
    onLevelSelected: (shapeIndex: Int, levelId: Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: LevelSelectViewModel = viewModel(
        factory = LevelSelectViewModelFactory(levelRepository, progressRepository, dailyChallengeRepository)
    )
    val uiState by viewModel.uiState.collectAsState()
    var selectedShapeIndex by remember { mutableIntStateOf(0) }

    val completedForCurrentShape = remember(uiState.completedSurfaceLevels, selectedShapeIndex) {
        uiState.completedSurfaceLevels
            .filter { it.startsWith("${selectedShapeIndex}_") }
            .mapNotNull { it.substringAfter("_").toIntOrNull() }
            .toSet()
    }
    val totalLevels = uiState.totalSurfaceLevels

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundGradient)
    ) {
        // Header
        SurfaceHeader(
            completedCount = completedForCurrentShape.size,
            totalCount = totalLevels,
            onBack = onBack
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Shape selector tabs
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(SurfaceShapesList.size) { index ->
                val shape = SurfaceShapesList[index]
                val isSelected = selectedShapeIndex == index
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) Color(0xFF6366F1) else Color(0xFF1E293B))
                        .border(
                            width = 1.dp,
                            color = if (isSelected) Color(0xFF8B5CF6) else Color.White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { selectedShapeIndex = index }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = shape.icon, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = shape.name,
                            color = if (isSelected) Color.White else Color(0xFF94A3B8),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Text(
                        text = "${SurfaceShapesList[selectedShapeIndex].icon} ${SurfaceShapesList[selectedShapeIndex].name}",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = SurfaceShapesList[selectedShapeIndex].description,
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            items(
                count = totalLevels,
                key = { "${selectedShapeIndex}_${it + 1}" }
            ) { index ->
                val levelNumber = index + 1
                val isCompleted = levelNumber in completedForCurrentShape
                val isCurrent = levelNumber == completedForCurrentShape.size + 1

                SurfaceLevelCard(
                    levelNumber = levelNumber,
                    isCompleted = isCompleted,
                    isCurrent = isCurrent,
                    onClick = { onLevelSelected(selectedShapeIndex, levelNumber) }
                )
            }
        }
    }
}

@Composable
private fun SurfaceHeader(
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

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Modo Superficies 3D",
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
                    .background(SurfaceProgressGradient)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "$completedCount / $totalCount Niveles resueltos",
            color = Color(0xFFA5B4FC),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SurfaceLevelCard(
    levelNumber: Int,
    isCompleted: Boolean,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1E293B))
            .border(
                width = 2.dp,
                color = when {
                    isCompleted -> Color(0xFF10B981)
                    isCurrent -> Color(0xFF6366F1)
                    else -> Color.White.copy(alpha = 0.1f)
                },
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = levelNumber.toString(),
                color = if (isCurrent) Color(0xFFA5B4FC) else Color.White,
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
